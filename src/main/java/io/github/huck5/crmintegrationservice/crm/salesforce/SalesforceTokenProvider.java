package io.github.huck5.crmintegrationservice.crm.salesforce;

import io.github.huck5.crmintegrationservice.crm.TokenProvider;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

@Component
public class SalesforceTokenProvider implements TokenProvider {
  private static final String CONSUMER_KEY = System.getenv("SF_CONSUMER_KEY");
  private static final String SF_USER = System.getenv("SF_USER");
  private static final Path KEY_FILE = Path.of(
    System.getProperty("user.home"),
    ".secrets", "crm-integration-service", "server.key"
  );
  private static final Path CERT_FILE = Path.of(
    System.getProperty("user.home"),
    ".secrets", "crm-integration-service", "server.crt"
  );
  private static final String LOGIN_URL = System.getenv("SF_LOGIN_URL");

  private final JsonMapper jsonMapper;

  private SalesforceAccessToken accessToken;

  public SalesforceTokenProvider(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public SalesforceAccessToken getToken() {
    if (accessToken == null) {
      accessToken = requestAccessToken();
    }
    return accessToken;
  }

  @Override
  public SalesforceAccessToken refreshToken() {
    accessToken = requestAccessToken();
    return accessToken;
  }

  private SalesforceAccessToken requestAccessToken() {
    String body =
      "grant_type="
        + URLEncoder.encode(
        "urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
        + "&assertion="
        + URLEncoder.encode(getJwt().getTokenValue(), StandardCharsets.UTF_8);

    HttpRequest request;
    try {
      request = HttpRequest.newBuilder()
                           .uri(new URI(LOGIN_URL + "/services/oauth2/token"))
                           .header("Content-Type", "application/x-www-form-urlencoded")
                           .POST(HttpRequest.BodyPublishers.ofString(body))
                           .build();
    }
    catch (URISyntaxException e) {
      throw new RuntimeException("LOGIN_URL environment variable did not parse correctly", e);
    }

    HttpResponse<String> response;
    try (HttpClient client = HttpClient.newHttpClient()) {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Salesforce token request interrupted", e);
    }
    catch (IOException e) {
      throw new RuntimeException("Salesforce token request failed", e);
    }

    if (response.statusCode() != 200) {
      throw new IllegalStateException(
        "Salesforce token request failed: HTTP " + response.statusCode());
    }

    return jsonMapper.readValue(response.body(), SalesforceAccessToken.class);
  }

  private static Jwt getJwt() {
    RSAPrivateKey privateKey;
    RSAPublicKey publicKey;

    try (InputStream input = Files.newInputStream(KEY_FILE)) {
      privateKey = RsaKeyConverters.pkcs8().convert(input);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    try (InputStream input = Files.newInputStream(CERT_FILE)) {
      publicKey = RsaKeyConverters.x509().convert(input);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    NimbusJwtEncoder encoder = NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

    JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                                         .claim("iss", CONSUMER_KEY)
                                         .claim("sub", SF_USER)
                                         .claim("aud", LOGIN_URL)
                                         //TODO: Replace this with .expiresAt(Instant.now().plusSeconds(180))
                                         .claim("exp",
                                           Instant.now().plusSeconds(180))
                                         .build();
    JwtEncoderParameters params = JwtEncoderParameters.from(header, claimsSet);

    return encoder.encode(params);
  }
}
