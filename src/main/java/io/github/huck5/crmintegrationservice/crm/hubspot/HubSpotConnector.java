package io.github.huck5.crmintegrationservice.crm.hubspot;

import io.github.huck5.crmintegrationservice.crm.AbstractOAuthConnector;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import io.github.huck5.crmintegrationservice.crm.ContactDto;
import io.github.huck5.crmintegrationservice.crm.hubspot.mapper.HubSpotContactMapper;
import io.github.huck5.crmintegrationservice.crm.hubspot.model.HubSpotContact;
import io.github.huck5.crmintegrationservice.crm.hubspot.response.HubSpotErrorResponse;
import io.github.huck5.crmintegrationservice.crm.hubspot.response.HubSpotQueryResponse;
import io.github.huck5.crmintegrationservice.crm.response.ContactRetrievalResponse;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class HubSpotConnector extends AbstractOAuthConnector {
  protected static final String END_POINT = "https://api.hubapi.com";
  protected static final String CONTACTS_URI = "/crm/objects/2026-09/contacts";
  protected static final String EXPIRED_AUTHENTICATION = "EXPIRED_AUTHENTICATION";
  private final HttpClient httpClient;
  private final JsonMapper jsonMapper;

  public HubSpotConnector(HubSpotTokenProvider tokenProvider, HttpClient httpClient, JsonMapper jsonMapper) {
    super(tokenProvider);
    this.httpClient = httpClient;
    this.jsonMapper = jsonMapper;
  }

  public ContactRetrievalResponse getContacts() throws URISyntaxException {
    URI endpoint = new URI(
      END_POINT + CONTACTS_URI);

    HttpRequest request = buildGetRequest(endpoint);

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 401) {
        HubSpotErrorResponse hubSpotErrorResponse = new ObjectMapper().readValue(response.body(), HubSpotErrorResponse.class);
        if (EXPIRED_AUTHENTICATION.equalsIgnoreCase(hubSpotErrorResponse.getCategory())) {
          tokenProvider.refreshToken();
          response = httpClient.send(buildGetRequest(endpoint), HttpResponse.BodyHandlers.ofString());
        }
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("HubSpot request interrupted", e);
    }
    catch (IOException e) {
      throw new RuntimeException("HubSpot request failed", e);
    }

    if (response.statusCode() != 200) {
      throw new IllegalStateException(
        "HubSpot request failed: HTTP " + response.statusCode());
    }

    HubSpotQueryResponse<HubSpotContact> queryResponse = jsonMapper.readValue(response.body(),
      new TypeReference<>() {
      });

    return HubSpotContactMapper.map(queryResponse);
  }
}
