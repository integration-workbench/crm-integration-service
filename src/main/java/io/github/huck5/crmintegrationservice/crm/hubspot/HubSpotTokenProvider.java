package io.github.huck5.crmintegrationservice.crm.hubspot;

import io.github.huck5.crmintegrationservice.crm.TokenProvider;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class HubSpotTokenProvider implements TokenProvider {
  private static final URI TOKEN_ENDPOINT = URI.create("https://api.hubapi.com/oauth/2026-09/token");

  private final JsonMapper jsonMapper;
  private final HttpClient httpClient;
  private final String clientId;
  private final String clientSecret;
  private final Clock clock;
  private String refreshToken;
  private Instant expiresAt;
  private HubSpotAccessToken accessToken;

  public HubSpotTokenProvider(
      JsonMapper jsonMapper,
      HttpClient httpClient,
      Clock clock,
      @Value("${HS_CLIENT_ID}") String clientId,
      @Value("${HS_CLIENT_SECRET}") String clientSecret,
      @Value("${HS_REFRESH_TOKEN}") String refreshToken) {
    this.jsonMapper = jsonMapper;
    this.httpClient = httpClient;
    this.clock = clock;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.refreshToken = refreshToken;
  }

  @Override
  public HubSpotAccessToken getToken() {
    if (accessToken == null || !Instant.now(clock).plusSeconds(30).isBefore(expiresAt)) {
      accessToken = refreshAccessToken();
    }
    return accessToken;
  }

  @Override
  public HubSpotAccessToken refreshToken() {
    accessToken = refreshAccessToken();
    return accessToken;
  }

  private HubSpotAccessToken refreshAccessToken() {
    String body =
        "grant_type=refresh_token"
            + "&client_id="
            + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
            + "&client_secret="
            + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
            + "&refresh_token="
            + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(TOKEN_ENDPOINT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response;
    try  {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("HubSpot token request interrupted", e);
    } catch (IOException e) {
      throw new RuntimeException("HubSpot token request failed", e);
    }

    if (response.statusCode() != 200) {
      throw new IllegalStateException("HubSpot token request failed: HTTP " + response.statusCode());
    }

    HubSpotAccessToken token = jsonMapper.readValue(response.body(), HubSpotAccessToken.class);
    expiresAt = Instant.now(clock).plusSeconds(token.getExpiresIn());

    if (token.getRefreshToken() != null && !token.getRefreshToken().isBlank()) {
      refreshToken = token.getRefreshToken();
    }

    return token;
  }
}
