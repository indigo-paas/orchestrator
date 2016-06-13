package it.reply.orchestrator.service;

import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Provider;
import it.reply.orchestrator.exception.service.DeploymentException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@PropertySource("classpath:cmdb/cmdb.properties")
public class CmdbServiceImpl implements CmdbService {

  @Autowired
  private RestTemplate restTemplate;

  @Value("${cmdb.url}")
  private String url;

  @Value("${service.id}")
  private String serviceIdUrlPath;

  @Value("${provider.id}")
  private String providerIdUrlPath;

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public it.reply.orchestrator.dto.cmdb.Service getServiceById(String id) {

    ResponseEntity<it.reply.orchestrator.dto.cmdb.Service> response = restTemplate.getForEntity(
        url.concat(serviceIdUrlPath).concat(id), it.reply.orchestrator.dto.cmdb.Service.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find service <" + id + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public Provider getProviderById(String id) {
    ResponseEntity<Provider> response =
        restTemplate.getForEntity(url.concat(providerIdUrlPath).concat(id), Provider.class);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response.getBody();
    }
    throw new DeploymentException("Unable to find provider <" + id + "> in the CMDB."
        + response.getStatusCode().toString() + " " + response.getStatusCode().getReasonPhrase());
  }

  @Override
  public List<Image> getImagesByProvider(String providerId) {
    providerId = "STUB:" + providerId;
    // FIXME: Stub
    return Arrays.asList(
        new Image().withImageName("indigodatacloud/ubuntu-sshd").withImageId(providerId + "/5")
            .withArchitecture("x86_64").withType("linux").withDistribution("ubuntu")
            .withVersion("14.04"),
        new Image().withImageName("linux-ubuntu-14.04").withImageId(providerId + "/5")
            .withArchitecture("x86_64").withType("linux").withDistribution("ubuntu")
            .withVersion("14.04"),
        new Image().withImageName("indigodatacloudapps/docker-galaxy")
            .withImageId(providerId + "/13"));
  }

}
