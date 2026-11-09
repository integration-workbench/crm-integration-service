package io.github.huck5.crmintegrationservice.crm.hubspot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import io.github.huck5.crmintegrationservice.crm.ContactDto;
import io.github.huck5.crmintegrationservice.crm.CrmSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class HubSpotConnectorTest {
  private static final String CONTACTS_JSON =
    """
      {
        "results": [
          {
            "id": "283634468816",
            "properties": {
              "email": "maria@example.com",
              "firstname": "Maria",
              "lastname": "Johnson"
            },
            "archived": false
          }
        ]
      }
      """;
  private static final String TEST_ACCESS_TOKEN = "test-access-token";
  private static final String REFRESH_ACCESS_TOKEN = "refresh-access-token";
  private static final int EXPIRATION_TIME = 1800;
  private static final String ACCESS_TOKEN_JSON =
    """
      {
        "access_token": "%s",
        "token_type": "bearer",
        "expires_in": %d,
        "refresh_token": "refresh-token"
      }
      """
      .formatted(TEST_ACCESS_TOKEN, EXPIRATION_TIME);
  private HubSpotConnector connector;
  @Mock
  private HttpClient httpClient;
  @Mock
  private HubSpotTokenProvider provider;
  @Mock
  private HttpResponse<String> response;
  @Mock
  private HttpResponse<String> expiredResponse;
  @Mock
  private HttpResponse<String> successResponse;
  private List<ContactDto> contacts;

  @BeforeEach
  void setUp() {
    JsonMapper jsonMapper = JsonMapper.builder().build();
    connector = new HubSpotConnector(provider, httpClient, jsonMapper);

    contacts = new ArrayList<>();
    contacts.add(ContactDto.builder()
                           .source(CrmSource.HUBSPOT)
                           .externalId("283634468816")
                           .firstName("Maria")
                           .lastName("Johnson")
                           .email("maria@example.com").build());

    HubSpotAccessToken accessToken =
      JsonMapper.builder().build().readValue(ACCESS_TOKEN_JSON, HubSpotAccessToken.class);
    when(provider.getToken()).thenReturn(accessToken);
  }

  @Test
  void getContacts_whenSuccessful_returnsContacts() throws IOException, InterruptedException, URISyntaxException {
    when(response.body()).thenReturn(CONTACTS_JSON);
    when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(200);

    List<ContactDto> result = connector.getContacts();
    assertEquals(contacts, result);

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

    verify(httpClient).send(captor.capture(), any());

    HttpRequest request = captor.getValue();
    assertEquals(HttpMethod.GET.toString(), request.method());
    assertEquals(HubSpotConnector.END_POINT + HubSpotConnector.CONTACTS_URI, request.uri().toString());
    assertEquals(List.of("Bearer " + TEST_ACCESS_TOKEN), request.headers().allValues("Authorization"));
  }

  @Test
  void getContacts_whenTokenExpired_refreshesAndReturnsContacts() throws IOException, InterruptedException, URISyntaxException {
    String replacementTokenJson =
      """
        {
          "access_token": "%s",
          "token_type": "bearer",
          "expires_in": %d,
          "refresh_token": "replacement-refresh-token"
        }
        """
        .formatted(REFRESH_ACCESS_TOKEN, EXPIRATION_TIME);
    HubSpotAccessToken originalToken =
      JsonMapper.builder().build().readValue(ACCESS_TOKEN_JSON, HubSpotAccessToken.class);
    HubSpotAccessToken replacementToken =
      JsonMapper.builder().build().readValue(replacementTokenJson, HubSpotAccessToken.class);

    when(provider.getToken()).thenReturn(originalToken, replacementToken);
    when(provider.refreshToken()).thenReturn(replacementToken);

    when(expiredResponse.statusCode()).thenReturn(401);
    when(expiredResponse.body())
      .thenReturn(
        """
          {
            "status": "error",
            "category": "EXPIRED_AUTHENTICATION",
            "message": "The OAuth token used to make this call expired."
          }
          """);

    when(successResponse.body()).thenReturn(CONTACTS_JSON);

    when(httpClient.<String>send(any(HttpRequest.class), any()))
      .thenReturn(expiredResponse)
      .thenReturn(successResponse);

    List<ContactDto> result = connector.getContacts();
    assertEquals(contacts, result);
    verify(provider).refreshToken();
    verify(httpClient, times(2)).send(any(HttpRequest.class), any());
  }

  @Test
  void getContacts_whenMissingEmail_producesNullEmail()
    throws IOException, InterruptedException, URISyntaxException {
    when(response.body()).thenReturn("""
      {
        "results": [
          {
            "id": "283634468816",
            "properties": {
              "firstname": "Maria",
              "lastname": "Johnson"
            }
          }
        ]
      }
      """);
    when(response.statusCode()).thenReturn(200);
    when(httpClient.<String>send(any(HttpRequest.class), any()))
      .thenReturn(response);

    List<ContactDto> result = connector.getContacts();

    ContactDto expected = ContactDto.builder()
                                    .source(CrmSource.HUBSPOT)
                                    .externalId("283634468816")
                                    .firstName("Maria")
                                    .lastName("Johnson")
                                    .build();

    assertEquals(List.of(expected), result);
  }
}
