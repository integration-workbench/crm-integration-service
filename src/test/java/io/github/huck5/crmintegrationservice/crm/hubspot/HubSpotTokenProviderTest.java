package io.github.huck5.crmintegrationservice.crm.hubspot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class HubSpotTokenProviderTest {
  private static final Instant START = Instant.parse("2026-01-01T12:00:00Z");
  private static final String TEST_ACCESS_TOKEN = "test-access-token";
  private static final String REFRESHED_ACCESS_TOKEN = "refreshed-access-token";
  private static final String REPLACED_ACCESS_TOKEN = "replacement-access-token";
  private static final int EXPIRATION_TIME = 1800;
  @Mock private Clock clock;
  @Mock private HttpClient httpClient;
  @Mock private HttpResponse<String> response;
  private HubSpotTokenProvider provider;

  @BeforeEach
  void setUp() throws IOException, InterruptedException {
    when(clock.instant()).thenReturn(START);

    provider =
        new HubSpotTokenProvider(
            JsonMapper.builder().build(), httpClient, clock, "test-client", "test-secret", "test-refresh-token");

    when(response.statusCode()).thenReturn(200);
    when(response.body())
        .thenReturn(
            """
            {
              "access_token": "%s",
              "token_type": "bearer",
              "expires_in": %d,
              "refresh_token": "replacement-refresh-token"
            }
            """
                .formatted(TEST_ACCESS_TOKEN, EXPIRATION_TIME));

    when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);
  }

  @Test
  void getToken_whenNoTokenCached_requestsToken() throws IOException, InterruptedException {
    HubSpotAccessToken token = provider.getToken();

    assertNotNull(token);
    assertEquals(TEST_ACCESS_TOKEN, token.getAccessToken());

    verify(httpClient, times(1)).send(any(HttpRequest.class), any());
  }

  @Test
  void getToken_whenCachedTokenValid_returnsCachedToken() throws IOException, InterruptedException {
    HubSpotAccessToken first = provider.getToken();
    HubSpotAccessToken second = provider.getToken();

    assertSame(first, second);

    verify(httpClient, times(1)).send(any(HttpRequest.class), any());
  }

  @Test
  void getToken_whenTokenExpired_refreshesToken() throws IOException, InterruptedException {
    HubSpotAccessToken first = provider.getToken();

    assertEquals(TEST_ACCESS_TOKEN, first.getAccessToken());
    verify(httpClient, times(1)).send(any(HttpRequest.class), any());

    // Advance beyond teh first token's expiry
    when(clock.instant()).thenReturn(START.plusSeconds(EXPIRATION_TIME));

    // Supply a different token for the next HTTP request
    when(response.body())
        .thenReturn(
            """
            {
              "access_token": "%s",
              "token_type": "bearer",
              "expires_in": %d,
              "refresh_token": "replacement-refresh-token"
            }
            """
                .formatted(REFRESHED_ACCESS_TOKEN, EXPIRATION_TIME));

    HubSpotAccessToken refreshed = provider.getToken();

    assertEquals(REFRESHED_ACCESS_TOKEN, refreshed.getAccessToken());
    assertSame(refreshed, provider.getToken());

    verify(httpClient, times(2)).send(any(HttpRequest.class), any());
  }

  @Test
  void getToken_whenWithinExpiryBuffer_refreshesToken() throws IOException, InterruptedException {
    HubSpotAccessToken first = provider.getToken();

    assertEquals(TEST_ACCESS_TOKEN, first.getAccessToken());
    verify(httpClient, times(1)).send(any(HttpRequest.class), any());

    // Advance beyond teh first token's expiry
    when(clock.instant()).thenReturn(START.plusSeconds(EXPIRATION_TIME - 25));

    // Supply a different token for the next HTTP request
    when(response.body())
        .thenReturn(
            """
            {
              "access_token": "%s",
              "token_type": "bearer",
              "expires_in": %d,
              "refresh_token": "replacement-refresh-token"
            }
            """
                .formatted(REFRESHED_ACCESS_TOKEN, EXPIRATION_TIME));

    HubSpotAccessToken refreshed = provider.getToken();

    assertEquals(REFRESHED_ACCESS_TOKEN, refreshed.getAccessToken());
    assertSame(refreshed, provider.getToken());

    verify(httpClient, times(2)).send(any(HttpRequest.class), any());
  }

  @Test
  void getToken_whenRefreshFails_throwsException() throws IOException, InterruptedException {
    // Acquire the initial token successfully.
    provider.getToken();

    // Expire it and make the refresh request fail.
    when(clock.instant()).thenReturn(START.plusSeconds(1801));
    when(response.statusCode()).thenReturn(400);

    IllegalStateException exception = assertThrows(IllegalStateException.class, provider::getToken);

    assertEquals("HubSpot token request failed: HTTP 400", exception.getMessage());

    verify(httpClient, times(2)).send(any(HttpRequest.class), any());
  }

  @Test
  void refreshToken_whenTokenCached_replacesCachedToken() throws IOException, InterruptedException {
    HubSpotAccessToken original = provider.getToken();

    when(response.body())
        .thenReturn(
            """
            {
              "access_token": "%s",
              "token_type": "bearer",
              "expires_in": %d,
              "refresh_token": "replacement-refresh-token"
            }
            """
                .formatted(REPLACED_ACCESS_TOKEN, EXPIRATION_TIME));

    HubSpotAccessToken replacement = provider.refreshToken();

    assertEquals(TEST_ACCESS_TOKEN, original.getAccessToken());
    assertEquals(REPLACED_ACCESS_TOKEN, replacement.getAccessToken());
    assertSame(replacement, provider.getToken());

    verify(httpClient, times(2)).send(any(HttpRequest.class), any());
  }
}
