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

import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.fedreg.Project;
import it.reply.orchestrator.dto.fedreg.UserGroup;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ServiceVersion("v2")
public class SlamServiceV2Impl implements SlamService {

  private SlamProperties slamProperties;

  private OAuth2TokenService oauth2TokenService;

  private RestTemplate restTemplate;

  /**
   * Creates a new SlamServiceImpl.
   *
   * @param slamProperties the SlamProperties to use
   * @param oauth2TokenService the OAuth2TokenService to use
   * @param restTemplateBuilder the RestTemplateBuilder to use
   */
  public SlamServiceV2Impl(SlamProperties slamProperties, OAuth2TokenService oauth2TokenService,
      RestTemplateBuilder restTemplateBuilder) {
    this.slamProperties = slamProperties;
    this.oauth2TokenService = oauth2TokenService;
    this.restTemplate = restTemplateBuilder.build();
  }

  // private SlamPreferences remapAttributes(UserGroup userGroup) {
  // SlamPreferences slamPreferences;

  // userGroup.getSlas.forEach(sla -> {
  // Preference preference;
  // preference.setCustomer(userGroup.getId());
  // preference.setId(null);
  // slamPreferences.setPreferences(null);

  // });
  // }

  @Override
  public SlamPreferences getCustomerPreferences(OidcTokenId tokenId, @Nullable String userGroup) {

    String slamCustomer =
        Optional.ofNullable(userGroup).orElse(oauth2TokenService.getOrganization(tokenId));

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(slamProperties.getUrl() + slamProperties.getCustomerPreferencesPath())
        .buildAndExpand(slamCustomer).normalize().toUri();

    SSLContext sslContext = null;

    CloseableHttpClient httpClient = HttpClients.custom().setSSLContext(sslContext)
        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();

    HttpComponentsClientHttpRequestFactory factory =
        new HttpComponentsClientHttpRequestFactory(httpClient);
    RestTemplate restTemplate2 = new RestTemplate(factory);

    URI requestUriFedRegUserGroup = UriComponentsBuilder
        .fromHttpUrl("https://fedreg-dev.cloud.infn.it/fed-reg/api/v1/user_groups/")
        .queryParam("with_conn", "true").queryParam("name", slamCustomer)
        .queryParam("idp_endpoint", tokenId.getOidcEntityId().getIssuer()).build().normalize()
        .toUri();

    URI requestUriFedRegProject = UriComponentsBuilder
        .fromHttpUrl("https://fedreg-dev.cloud.infn.it/fed-reg/api/v1/projects/")
        .queryParam("with_conn", "true")
        .queryParam("user_group_uid", "f22c706974cd42ef9824dec311660506")
        .queryParam("provider_uid", "5da3cbaccf5940d9885e6a7fa7450137").build().normalize().toUri();

    List<UserGroup> userGroupCall =
        oauth2TokenService.executeWithClientForResult(tokenId, accessToken -> {
          HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUriFedRegUserGroup);
          if (accessToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
          }
          return restTemplate2.exchange(requestBuilder.build(),
              new ParameterizedTypeReference<List<UserGroup>>() {});
        }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();

    List<Project> projectCall =
        oauth2TokenService.executeWithClientForResult(tokenId, accessToken -> {
          HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUriFedRegProject);
          if (accessToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
          }
          return restTemplate2.exchange(requestBuilder.build(),
              new ParameterizedTypeReference<List<Project>>() {});
        }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();

    try {
      return oauth2TokenService.executeWithClientForResult(tokenId, accessToken -> {
        HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUri);
        if (accessToken != null) {
          requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return restTemplate.exchange(requestBuilder.build(), SlamPreferences.class);
      }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error fetching SLA for customer <" + slamCustomer + "> from SLAM.", ex);
    }
  }

}
