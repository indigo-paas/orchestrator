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

import com.google.common.collect.Lists;
import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.fedreg.UserGroup;
import it.reply.orchestrator.dto.slam.Preference;
import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.dto.slam.Priority;
import it.reply.orchestrator.dto.slam.Restrictions;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.dto.slam.Target;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import java.lang.reflect.Field;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ServiceVersion("v2")
public class SlamServiceV2Impl implements SlamService {

  private SlamProperties slamProperties;

  private OAuth2TokenService oauth2TokenService;

  private RestTemplate restTemplate;

  private static final Double DEFAULT_WEIGHT = 1.0;

  private static final List<String> ATTRIBUTES_TO_DISCARD =
      Lists.newArrayList("description", "perUser", "type", "usage", "uid", "service");

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

  private List<Preference> remapAttributes(UserGroup userGroup) {
    List<Preference> listOfPreference = new ArrayList<>();
    List<PreferenceCustomer> preferences = new ArrayList<>();
    List<String> serviceTypesPresent = new ArrayList<>();
    List<String> listOfServiceId = new ArrayList<>();
    userGroup.getSlas().forEach(slaFedReg -> {
      slaFedReg.getProjects().forEach(projectFedReg -> {
        projectFedReg.getQuotas().forEach(quotaFedReg -> {
          // Qua dovrei definire e riempire i singoli target
          String serviceType = quotaFedReg.getService().getType();
          String serviceId = quotaFedReg.getService().getUid();

          if (Boolean.FALSE.equals(quotaFedReg.getUsage())) {
            Priority priority = new Priority(slaFedReg.getUid(), serviceId, DEFAULT_WEIGHT);
            listOfServiceId.add(serviceId);

            if (serviceTypesPresent.contains(serviceType)) {
              preferences.forEach(preferencesElem -> {
                if (preferencesElem.getServiceType().equals(serviceType)) {
                  preferencesElem.getPriority().add(priority);
                }
              });
            } else {
              serviceTypesPresent.add(serviceType);
              List<Priority> listOfPriorities = new ArrayList<>();
              listOfPriorities.add(priority);
              PreferenceCustomer preferenceCustomer =
                  new PreferenceCustomer(serviceType, listOfPriorities);
              preferences.add(preferenceCustomer);
            }
          }
        });
      });
    });
    Preference preference = new Preference(userGroup.getUid(), preferences, null);
    listOfPreference.add(preference);
    return listOfPreference;
  }

  private List<Sla> remapAttributesForSla(UserGroup userGroup) {
    List<Sla> listOfSlas = new ArrayList<>();
    userGroup.getSlas().forEach(slaFedReg -> {
      slaFedReg.getProjects().forEach(projectFedReg -> {
        // could go outside this loop
        HashMap<it.reply.orchestrator.dto.fedreg.Service, HashMap<String, Restrictions>>
            mapForTargets = new HashMap<>();

        projectFedReg.getQuotas().forEach(quotaFedReg -> {
          if (Boolean.FALSE.equals(quotaFedReg.getUsage())) {
            Field[] fields = quotaFedReg.getClass().getDeclaredFields();
            HashMap<String, Restrictions> mapForRestrictions = new HashMap<>();
            if (mapForTargets.containsKey(quotaFedReg.getService())) {
              mapForRestrictions = mapForTargets.get(quotaFedReg.getService());
            }

            for (Field f : fields) {
              f.setAccessible(true);
              String field = f.getName();
              // Retrive the value of the filed f of the object quotaFedReg
              Object valueObject;
              try {
                valueObject = f.get(quotaFedReg);
              } catch (IllegalArgumentException | IllegalAccessException e) {
                valueObject = null;
                e.printStackTrace();
              }

              if (ATTRIBUTES_TO_DISCARD.contains(field) || valueObject == null) {
                continue;
              }

              // Fix casting of value (it depends on f.type)
              Integer value = (Integer) valueObject;
              Restrictions restriction = new Restrictions();
              restriction.setInstanceGuaranteed(null);
              restriction.setInstanceLimit(null);
              if (mapForRestrictions.containsKey(field)) {
                restriction = mapForRestrictions.get(field);
              }
              if (Boolean.FALSE.equals(quotaFedReg.getPerUser())) {
                restriction.setTotalGuaranteed(value);
                restriction.setTotalLimit(value);
              } else {
                restriction.setUserGuaranteed(value);
                restriction.setUserLimit(value);
              }
              mapForRestrictions.put(field, restriction);
            }

            mapForTargets.put(quotaFedReg.getService(), mapForRestrictions);
          }

        });

        List<it.reply.orchestrator.dto.slam.Service> slamServices = new ArrayList<>();
        mapForTargets.keySet().forEach(service -> {
          HashMap<String, Restrictions> value = mapForTargets.get(service);
          List<Target> targets = new ArrayList<>();
          value.keySet().forEach(key -> {
            Target target = new Target(key, null, value.get(key));
            targets.add(target);
          });
          it.reply.orchestrator.dto.slam.Service slamService =
              new it.reply.orchestrator.dto.slam.Service(service.getType(), service.getUid(),
                  targets);
          slamServices.add(slamService);
        });
        Sla sla = new Sla(userGroup.getUid(), projectFedReg.getProvider().getUid(),
            new SimpleDateFormat("yyyy-MM-dd").format(slaFedReg.getStartDate()),
            new SimpleDateFormat("yyyy-MM-dd").format(slaFedReg.getEndDate()), slamServices,
            slaFedReg.getUid());
        listOfSlas.add(sla);
      });
    });
    return listOfSlas;
  }

  @Override
  public SlamPreferences getCustomerPreferences(OidcTokenId tokenId, @Nullable String userGroup) {

    String slamCustomer =
        Optional.ofNullable(userGroup).orElse(oauth2TokenService.getOrganization(tokenId));

    // URI requestUri = UriComponentsBuilder
    //     .fromHttpUrl(slamProperties.getUrl() + slamProperties.getCustomerPreferencesPath())
    //     .buildAndExpand(slamCustomer).normalize().toUri();

    SSLContext sslContext = null;

    CloseableHttpClient httpClient = HttpClients.custom().setSSLContext(sslContext)
        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();

    HttpComponentsClientHttpRequestFactory factory =
        new HttpComponentsClientHttpRequestFactory(httpClient);
    RestTemplate restTemplate2 = new RestTemplate(factory);

    URI requestUriFedRegUserGroup = UriComponentsBuilder
        .fromHttpUrl(slamProperties.getUrl() + slamProperties.getCustomerPreferencesPath())
        .queryParam("with_conn", true).queryParam("name", slamCustomer)
        .queryParam("idp_endpoint", tokenId.getOidcEntityId().getIssuer())
        .queryParam("provider_status", "active").build().normalize()
        .toUri();

    // URI requestUriFedRegProject = UriComponentsBuilder
    //     .fromHttpUrl("https://fedreg-dev.cloud.infn.it/fed-reg/api/v1/projects/")
    //     .queryParam("with_conn", "true")
    //     .queryParam("user_group_uid", "ddb06273f5d34473a7f5742bd531a8f4")
    //     .queryParam("provider_uid", "ee70b67629da4a768adf03fe75f6c845").build().normalize().toUri();

    List<UserGroup> userGroupCall =
        oauth2TokenService.executeWithClientForResult(tokenId, accessToken -> {
          HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUriFedRegUserGroup);
          if (accessToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
          }
          return restTemplate2.exchange(requestBuilder.build(),
              new ParameterizedTypeReference<List<UserGroup>>() {});
        }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();

    // List<Project> projectCall =
    //     oauth2TokenService.executeWithClientForResult(tokenId, accessToken -> {
    //       HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUriFedRegProject);
    //       if (accessToken != null) {
    //         requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    //       }
    //       return restTemplate2.exchange(requestBuilder.build(),
    //           new ParameterizedTypeReference<List<Project>>() {});
    //     }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();

    // SlamPreferences testSlamPreferences = remapAttributes(userGroupCall.get(0));
    // remapAttributesForSla(userGroupCall.get(0));

    SlamPreferences slamPreferences = new SlamPreferences(remapAttributes(userGroupCall.get(0)),
        remapAttributesForSla(userGroupCall.get(0)));
    return slamPreferences;
    // try {
    //   return oauth2TokenService.executeWithClientForResult(tokenId, accessToken -> {
    //     HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUri);
    //     if (accessToken != null) {
    //       requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    //     }
    //     return restTemplate.exchange(requestBuilder.build(), SlamPreferences.class);
    //   }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();
    // } catch (RestClientException ex) {
    //   throw new DeploymentException(
    //       "Error fetching SLA for customer <" + slamCustomer + "> from SLAM.", ex);
    // }
  }

}
