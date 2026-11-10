package io.github.huck5.crmintegrationservice.crm.salesforce;

import io.github.huck5.crmintegrationservice.crm.AbstractOAuthConnector;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.github.huck5.crmintegrationservice.crm.salesforce.mapper.SalesforceContactMapper;
import io.github.huck5.crmintegrationservice.crm.salesforce.model.SalesforceContact;
import io.github.huck5.crmintegrationservice.crm.response.ContactRetrievalResponse;
import io.github.huck5.crmintegrationservice.crm.salesforce.response.SalesforceQueryResponse;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SalesforceConnector extends AbstractOAuthConnector {
  protected static final String CONTACTS_URI =
    "/services/data/v66.0/query?q=SELECT+Id,FirstName,LastName,Email+FROM+Contact";
  private final JsonMapper jsonMapper;
  private final HttpClient httpClient;

  public SalesforceConnector(SalesforceTokenProvider tokenProvider, JsonMapper jsonMapper, HttpClient httpClient) {
    super(tokenProvider);
    this.jsonMapper = jsonMapper;
    this.httpClient = httpClient;
  }

  public ContactRetrievalResponse getContacts() throws URISyntaxException {
    SalesforceAccessToken token = (SalesforceAccessToken) tokenProvider.getToken();
    URI endpoint = new URI(token.getInstanceUrl() + CONTACTS_URI);

    HttpRequest request = HttpRequest.newBuilder()
                                     .uri(endpoint)
                                     .header("Authorization", "Bearer " + token.getAccessToken())
                                     .header("Sforce-Query-Options", "batchSize=200")
                                     .GET()
                                     .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 401
        && "INVALID_SESSION_ID".equals(jsonMapper.readTree(response.body()).path(0).path("errorCode").asString())) {
        request =
          HttpRequest.newBuilder()
                     .uri(endpoint)
                     .header("Authorization", "Bearer " + tokenProvider.refreshToken().getAccessToken())
                     .GET()
                     .build();

        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Salesforce request interrupted", e);
    }
    catch (IOException e) {
      throw new RuntimeException("Salesforce request failed", e);
    }

    if (response.statusCode() != 200) {
      throw new IllegalStateException(
        "Salesforce request failed: HTTP " + response.statusCode());
    }

    SalesforceQueryResponse<SalesforceContact> queryResponse = jsonMapper.readValue(response.body(), new TypeReference<>() {
    });

    return SalesforceContactMapper.map(queryResponse);
  }
}
