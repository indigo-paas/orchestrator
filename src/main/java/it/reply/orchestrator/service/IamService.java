package it.reply.orchestrator.service;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.io.IOException;

import org.mitre.oauth2.model.RegisteredClient;

public class IamService {

  private static final String iamClientScopes = "openid profile email offline_access iam:admin.write";
  //private static final String iamConfig = "https://iotwins-iam.cloud.cnaf.infn.it/.well-known/openid-configuration";

  public String getEndpoint(RestTemplate restTemplate, String iamUrl, String endpointName){
    String iamConfig = iamUrl + ".well-known/openid-configuration";
    ResponseEntity<String> response = restTemplate.getForEntity(iamConfig, String.class);
    String responseBody = "";
    if (response.getStatusCode() == HttpStatus.OK) {
      responseBody = response.getBody();
      System.out.println("Corpo della risposta: " + responseBody);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        // Estrai il campo "registration_endpoint" dal JSON
        String registration_endpoint = jsonNode.get(endpointName).asText();

        // Stampa il valore di registration_endpoint
        System.out.println("endpoint: " + registration_endpoint);
        return registration_endpoint;
      }
      catch (IOException e) {
        e.printStackTrace();
        return responseBody;
      }
    }
    else {
      return "La richiesta non ha avuto successo. Status code: " + response.getStatusCode();
    }
  }
  

  public String getToken(RestTemplate restTemplate, String iamClientId, String iamClientSecret, String iamClientScopes, String iamTokenEndpoint){

      // Imposta l'autenticazione di base nell'intestazione "Authorization"
      HttpHeaders headers = new HttpHeaders();
      String auth = iamClientId + ":" + iamClientSecret;
      byte[] authBytes = auth.getBytes();
      byte[] base64CredsBytes = Base64.getEncoder().encode(authBytes);
      String base64Creds = new String(base64CredsBytes);
      headers.add("Authorization", "Basic " + base64Creds);

      // Crea un oggetto MultiValueMap per i dati del corpo
      MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
      requestBody.add("grant_type", "client_credentials");
      requestBody.add("scope", iamClientScopes);

      // Crea un oggetto HttpEntity che contiene le intestazioni e il corpo
      HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

      // Esegui la richiesta HTTP POST
      ResponseEntity<String> responseEntity = restTemplate.exchange(
          iamTokenEndpoint,
          HttpMethod.POST,
          requestEntity,
          String.class
      );

      // Verifica la risposta
      if (responseEntity.getStatusCode() == HttpStatus.OK) {
          String responseBody = responseEntity.getBody();
          System.out.println("Risposta del server: " + responseBody);

          ObjectMapper objectMapper = new ObjectMapper();

          try {
          // Analizza il JSON in un oggetto JsonNode
          JsonNode jsonNode = objectMapper.readTree(responseBody);

          // Estrai il campo "access_token" dal JSON
          String access_token = jsonNode.get("access_token").asText();

          // Stampa il valore di access_token
          System.out.println("access_token: " + access_token);
          return access_token;
          }
          catch (IOException e) {
              e.printStackTrace();
              return "";
          }
      } else {
          return "La richiesta POST non ha avuto successo. Status code: " + responseEntity.getStatusCode();
      }
  }

  public String createClient(RestTemplate restTemplate, String iamRegistration){
      //String iamRegistration = "https://iotwins-iam.cloud.cnaf.infn.it/iam/api/client-registration";

      String jsonRequestBody = "{\n" +
      "  \"redirect_uris\": [\n" +
      "    \"https://another.client.example/oidc\"\n" +
      "  ],\n" +
      "  \"client_name\": \"test_giommi_registration_2\",\n" +
      "  \"contacts\": [\n" +
      "    \"luca.giommi@cnaf.infn.it\"\n" +
      "  ],\n" +
      "  \"token_endpoint_auth_method\": \"client_secret_basic\",\n" +
      "  \"scope\": \"openid email profile offline_access\",\n" +
      "  \"grant_types\": [\n" +
      "    \"refresh_token\",\n" +
      "    \"authorization_code\"\n" +
      "  ],\n" +
      "  \"response_types\": [\n" +
      "    \"code\"\n" +
      "  ]\n" +
      "}";

      // Crea un oggetto HttpHeaders per specificare il tipo di contenuto JSON
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Crea l'oggetto HttpEntity che contiene il corpo della richiesta e le intestazioni
      HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequestBody, headers);

      // URL del servizio REST che riceve la richiesta POST
      String postUrl = iamRegistration; // Sostituisci con l'URL corretto


      // Effettua la richiesta POST
      ResponseEntity<String> responseEntity = restTemplate.exchange(
          postUrl,
          HttpMethod.POST,
          requestEntity,
          String.class
      );

      String responseBody = responseEntity.getBody();
      System.out.println("Risposta del server: " + responseBody);

      ObjectMapper objectMapper = new ObjectMapper();

      try {
          // Analizza il JSON in un oggetto JsonNode
          JsonNode jsonNode = objectMapper.readTree(responseBody);

          // Estrai il campo "client_id" dal JSON
          String clientId = jsonNode.get("client_id").asText();

          // Utilizza il valore di clientId
          System.out.println("client_id: " + clientId);
          return clientId;
          }
          catch (IOException e) {
              e.printStackTrace();
              return "";
          }

      //if (responseEntity.getStatusCode() == HttpStatus.OK) {
      //String responseBody = responseEntity.getBody();
      //System.out.println("Risposta del server: " + responseBody);
      //} else {
      //System.err.println("La richiesta POST non ha avuto successo. Status code: " + responseEntity.getStatusCode());
      //}

      //deleteClient();
  }
    
    public void deleteClient(String clientId, String iamUrl, String token){
      // Crea un oggetto HttpHeaders e aggiungi il token come autorizzazione
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + token);

      // Crea l'oggetto HttpEntity che contiene l'intestazione con il token
      HttpEntity<?> requestEntity = new HttpEntity<>(headers);

      // URL del servizio REST su cui eseguire la richiesta DELETE
      String deleteUrl = iamUrl + "iam/api/clients/" + clientId; // Sostituisci con l'URL corretto

      // Crea un oggetto RestTemplate
      RestTemplate restTemplate = new RestTemplate();

      // Effettua la richiesta DELETE
      ResponseEntity<String> responseEntity = restTemplate.exchange(
          deleteUrl,
          HttpMethod.DELETE,
          requestEntity,
          String.class
      );

      // Verifica la risposta
      if (responseEntity.getStatusCode() == HttpStatus.NO_CONTENT) {
          System.out.println("La richiesta DELETE ha avuto successo.");
      } else {
          System.err.println("La richiesta DELETE non ha avuto successo. Status code: " + responseEntity.getStatusCode());
      }
  }
}