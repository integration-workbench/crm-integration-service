package io.github.huck5.crmintegrationservice.crm;

import java.net.URI;
import java.net.http.HttpRequest;

public abstract class AbstractOAuthConnector {
  protected final TokenProvider tokenProvider;

  protected AbstractOAuthConnector(TokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  protected HttpRequest buildGetRequest(URI endpoint) {
    return HttpRequest.newBuilder()
        .uri(endpoint)
        .header("Authorization", "Bearer " + tokenProvider.getToken().getAccessToken())
        .GET()
        .build();
  }
}
