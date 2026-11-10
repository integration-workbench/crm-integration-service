package io.github.huck5.crmintegrationservice.crm.salesforce;

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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesforceConnectorTest {
  private static final String TEST_ACCESS_TOKEN = "test-access-token";
  private static final String REFRESH_ACCESS_TOKEN = "refresh-access-token";
  private static final String CONTACTS_JSON =
    """
      {
        "totalSize": 1,
        "done": true,
        "records": [
          {
            "Id": "003000000000001",
            "FirstName": "Maria",
            "LastName": "Johnson",
            "Email": "maria@example.com"
          }
        ]
      }
      """;
  private static final String TEST_INSTANCE_URL = "https://test-instance.my.salesforce.com";
  private static final String ACCESS_TOKEN_JSON =
    """
      {
        "access_token": "%s",
        "token_type": "Bearer",
        "instance_url": "%s"
      }
      """
      .formatted(TEST_ACCESS_TOKEN, TEST_INSTANCE_URL);
  private static final String INVALID_SESSION_RESPONSE_JSON = """
    [
      {
        "message": "Session expired or invalid",
        "errorCode": "INVALID_SESSION_ID"
      }
    ]
    """;
  private SalesforceConnector connector;
  @Mock
  private HttpClient httpClient;
  @Mock
  private HttpResponse<String> response;
  @Mock
  private SalesforceTokenProvider provider;
  @Mock
  private HttpResponse<String> expiredResponse;
  @Mock
  private HttpResponse<String> successResponse;
  private List<ContactDto> contacts;

  @BeforeEach
  void setUp() {
    JsonMapper jsonMapper = new JsonMapper();
    connector = new SalesforceConnector(provider, jsonMapper, httpClient);
    SalesforceAccessToken accessToken =
      JsonMapper.builder().build().readValue(ACCESS_TOKEN_JSON, SalesforceAccessToken.class);

    contacts = new ArrayList<>();
    contacts.add(ContactDto.builder()
                           .source(CrmSource.SALESFORCE)
                           .externalId("003000000000001")
                           .firstName("Maria")
                           .lastName("Johnson")
                           .email("maria@example.com").build());

    when(provider.getToken()).thenReturn(accessToken);
  }

  @Test
  void getContacts_whenSuccessful_returnsContacts() throws IOException, InterruptedException, URISyntaxException {
    when(response.body()).thenReturn(CONTACTS_JSON);
    when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(200);

    List<ContactDto> result = connector.getContacts().contacts();
    // Verify response data, requested URI and cached token header.
    assertEquals(contacts, result);

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

    verify(httpClient).send(captor.capture(), any());
    HttpRequest request = captor.getValue();
    assertEquals(HttpMethod.GET.toString(), request.method());
    assertEquals(TEST_INSTANCE_URL + SalesforceConnector.CONTACTS_URI, request.uri().toString());
    assertEquals(List.of("Bearer " + TEST_ACCESS_TOKEN), request.headers().allValues("Authorization"));
  }

  @Test
  void getContacts_withUnauthorizedResponse_retriesWithReplacementToken()
    throws IOException, InterruptedException, URISyntaxException {
    String replacementTokenJson =
      """
        {
          "access_token": "%s",
          "token_type": "Bearer",
          "instance_url": "%s"
        }
        """
        .formatted(REFRESH_ACCESS_TOKEN, TEST_INSTANCE_URL);

    SalesforceAccessToken originalToken =
      JsonMapper.builder().build().readValue(ACCESS_TOKEN_JSON, SalesforceAccessToken.class);
    SalesforceAccessToken replacementToken =
      JsonMapper.builder().build().readValue(replacementTokenJson, SalesforceAccessToken.class);

    when(provider.getToken()).thenReturn(originalToken, replacementToken);
    when(provider.refreshToken()).thenReturn(replacementToken);

    when(expiredResponse.statusCode()).thenReturn(401);
    when(expiredResponse.body())
      .thenReturn(
        INVALID_SESSION_RESPONSE_JSON);

    when(successResponse.body()).thenReturn(CONTACTS_JSON);
    when(successResponse.statusCode()).thenReturn(200);

    when(httpClient.<String>send(any(HttpRequest.class), any()))
      .thenReturn(expiredResponse)
      .thenReturn(successResponse);

    List<ContactDto> result = connector.getContacts().contacts();
    assertEquals(contacts, result);
    verify(provider).refreshToken();
    verify(httpClient, times(2)).send(any(HttpRequest.class), any());
  }

  @Test
  void getContacts_whenRetryAlsoUnauthorized_surfacesFailureWithoutFurtherRetry() throws IOException, InterruptedException {
    // Both GETs return 401.
    // Verify one token replacement, exactly two GETs
    // and the failure exposed to the caller.
    String replacementTokenJson =
      """
        {
          "access_token": "%s",
          "token_type": "Bearer",
          "instance_url": "%s"
        }
        """
        .formatted(REFRESH_ACCESS_TOKEN, TEST_INSTANCE_URL);

    SalesforceAccessToken originalToken =
      JsonMapper.builder().build().readValue(ACCESS_TOKEN_JSON, SalesforceAccessToken.class);
    SalesforceAccessToken replacementToken =
      JsonMapper.builder().build().readValue(replacementTokenJson, SalesforceAccessToken.class);

    when(provider.getToken()).thenReturn(originalToken, replacementToken);
    when(provider.refreshToken()).thenReturn(replacementToken);

    when(expiredResponse.statusCode()).thenReturn(401);
    when(expiredResponse.body())
      .thenReturn(
        INVALID_SESSION_RESPONSE_JSON);

    when(httpClient.<String>send(any(HttpRequest.class), any()))
      .thenReturn(expiredResponse);

    assertThrows(IllegalStateException.class, () -> connector.getContacts());

    verify(provider).refreshToken();
    verify(httpClient, times(2)).send(any(HttpRequest.class), any());
  }

  @Test
  void getContacts_withNon401Error_doesNotReplaceTokenOrRetry() throws IOException, InterruptedException {
    // For example, a 403 permission failure.
    // Verify one GET, no replacement and the surfaced failure.
    when(expiredResponse.statusCode()).thenReturn(403);

    when(httpClient.<String>send(any(HttpRequest.class), any()))
      .thenReturn(expiredResponse);

    assertThrows(IllegalStateException.class, () -> connector.getContacts());

    verify(httpClient).send(any(HttpRequest.class), any());
    verify(provider, times(0)).refreshToken();
  }

  @Test
  void getContacts_whenTokenReplacementFails_surfacesFailureWithoutRetry()
    throws IOException, InterruptedException {
    IllegalStateException failure =
      new IllegalStateException("Token replacement failed");

    when(expiredResponse.statusCode()).thenReturn(401);
    when(expiredResponse.body()).thenReturn(INVALID_SESSION_RESPONSE_JSON);

    when(httpClient.<String>send(any(HttpRequest.class), any()))
      .thenReturn(expiredResponse);

    when(provider.refreshToken()).thenThrow(failure);

    IllegalStateException thrown =
      assertThrows(IllegalStateException.class, () -> connector.getContacts());

    assertSame(failure, thrown);
    verify(httpClient).send(any(HttpRequest.class), any());
    verify(provider).refreshToken();
  }

  @Test
  void getContacts_whenMissingEmail_producesNullEmail() throws IOException, InterruptedException, URISyntaxException {
    when(response.body()).thenReturn("""
      {
        "totalSize": 1,
        "done": true,
        "records": [
          {
            "Id": "003000000000001",
            "FirstName": "Maria",
            "LastName": "Johnson"
          }
        ]
      }
      """);
    when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(200);

    List<ContactDto> result = connector.getContacts().contacts();

    assertNull(result.getFirst().getEmail());

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

    verify(httpClient).send(captor.capture(), any());
    HttpRequest request = captor.getValue();
    assertEquals(HttpMethod.GET.toString(), request.method());
    assertEquals(TEST_INSTANCE_URL + SalesforceConnector.CONTACTS_URI, request.uri().toString());
    assertEquals(List.of("Bearer " + TEST_ACCESS_TOKEN), request.headers().allValues("Authorization"));
  }
}
