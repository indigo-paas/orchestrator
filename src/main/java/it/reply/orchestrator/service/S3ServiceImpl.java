/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.service.deployment.providers.CredentialProviderService;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Service
@Slf4j
public class S3ServiceImpl implements S3Service {

  @Autowired
  private CredentialProviderService credProvServ;

  private static final String S3_TOSCA_NODE_TYPE = "tosca.nodes.indigo.S3Bucket";
  private static final String BUCKET_NAME = "bucket_name";
  private static final String AWS_ACCESS_KEY = "aws_access_key";
  private static final String AWS_SECRET_KEY = "aws_secret_key";
  private static final String S3_URL = "s3_url";
  private static final String AWS_REGION = "us-east-1";

  /**
   * Create a bucket.
   *
   * @param s3 the S3 Client
   * @param bucketName the name of the bucket to create
   */
  private static void createBucket(S3Client s3, String bucketName) {
    CreateBucketRequest createBucketRequest =
        CreateBucketRequest.builder().bucket(bucketName).build();
    s3.createBucket(createBucketRequest);
  }

  /**
   * Delete a bucket.
   *
   * @param s3 the S3 Client
   * @param bucketName the name of the bucket to delete
   */
  public static void deleteBucket(S3Client s3, String bucketName) {
    DeleteBucketRequest deleteBucketRequest =
        DeleteBucketRequest.builder().bucket(bucketName).build();
    s3.deleteBucket(deleteBucketRequest);
  }

  /**
   * Delete all buckets.
   *
   * @param resources object used to make HTTP requests
   * @param accessToken the identity provider
   */
  public void deleteAllBuckets(Map<Boolean, Set<Resource>> resources, String accessToken) {
    for (Resource resource : resources.get(false)) {
      if (resource.getToscaNodeType().equals(S3_TOSCA_NODE_TYPE)) {
        Map<String, String> resourceMetadata = resource.getMetadata();
        if (resourceMetadata != null) {
          String bucketName = resourceMetadata.get(BUCKET_NAME);
          String s3Url = resourceMetadata.get(S3_URL);
          S3Client s3 = setupS3Client(s3Url, accessToken);

          // Delete S3 bucket
          try {
            LOG.info("Deleting bucket with name {}", bucketName);
            deleteBucket(s3, bucketName);
            LOG.info("Bucket {} successfully deleted", bucketName);
          } catch (NoSuchBucketException e) {
            LOG.warn("Bucket {} was not found", bucketName);
          } catch (Exception e) {
            LOG.error(e.getMessage());
            throw e;
          }
        } else {
          LOG.info("Found node of type {} but no bucket info is registered in metadata",
              S3_TOSCA_NODE_TYPE);
        }
      }
    }
  }

  /**
   * Create the S3 Client object.
   *
   * @param s3Url the S3 URL
   * @param accessToken the user's accessToken
   * @return the S3Client object
   */
  public S3Client setupS3Client(String s3Url, String accessToken) {
    S3Client s3 = null;
    String accessKeyId = null;
    String secretKey = null;
    if (s3Url != null) {
      // Configure S3 client with credentials
      try {
        Map<String, Object> vaultOutput =
            credProvServ.credentialProvider(s3Url.split("//")[1], accessToken);
        Map<String, String> s3Credentials = (Map<String, String>) vaultOutput.get("data");
        accessKeyId = s3Credentials.get(AWS_ACCESS_KEY);
        secretKey = s3Credentials.get(AWS_SECRET_KEY);

        s3 = S3Client.builder().endpointOverride(URI.create(s3Url)).region(Region.of(AWS_REGION))
            .forcePathStyle(true).credentialsProvider(StaticCredentialsProvider
                .create(AwsBasicCredentials.create(accessKeyId, secretKey)))
            .build();
      } catch (Exception e) {
        LOG.error(e.getMessage());
        throw e;
      }
    }
    return s3;
  }

  /**
   * Manage the creation of a bucket.
   *
   * @param bucketName the name of the bucket to create
   * @param accessToken the user's accessToken
   */
  public void manageBucketCreation(String bucketName, String s3Url, String accessToken) {
    S3Client s3 = setupS3Client(s3Url, accessToken);

    try {
      // Try to create a bucket
      createBucket(s3, bucketName);
      LOG.info("Bucket successfully created: {}", bucketName);
    } catch (Exception e) {
      LOG.error(e.getMessage());
      throw e;
    }
  }
}
