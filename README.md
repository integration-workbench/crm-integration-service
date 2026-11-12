# CRM Integration Service

A Java 21 / Spring Boot application that retrieves contacts from Salesforce and HubSpot and exposes them through a shared JSON response format.

[Record Service](https://github.com/integration-workbench/record-service) consumes these responses to import contacts into its local database.

## Requirements

- JDK 21.
- Network access for Maven dependencies and live CRM requests.
- Salesforce JWT bearer authentication configured for a development account.
- A HubSpot OAuth application and refresh token with permission to read contacts.

The Maven Wrapper is included; a separate Maven installation is not required.

## Local configuration

Supply these environment variables through your shell or IntelliJ run configuration:

| Variable | Purpose |
| --- | --- |
| `SF_CONSUMER_KEY` | Salesforce OAuth application's consumer key/client ID |
| `SF_USER` | Salesforce username used as the JWT subject |
| `SF_LOGIN_URL` | Salesforce login base URL, without a trailing slash |
| `HS_CLIENT_ID` | HubSpot OAuth application's client ID |
| `HS_CLIENT_SECRET` | HubSpot OAuth application's client secret |
| `HS_REFRESH_TOKEN` | Refresh token obtained during HubSpot authorization |

The Salesforce token provider also reads these files relative to the JVM's `user.home`:

- `.secrets/crm-integration-service/server.key`
- `.secrets/crm-integration-service/server.crt`

Keep credentials and signing files outside the repository. The application does not automatically load a `.env` file.

HubSpot configuration is required when the Spring application context starts. Salesforce signing files are read when a Salesforce token is requested.

The Salesforce connector uses the `instance_url` returned by the token exchange to select the API host.

## Run locally

After setting the required configuration, run from the repository root.

### Linux/macOS

```bash
./mvnw spring-boot:run
```

### Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run
```

The default local address is `http://localhost:8080`.

## Contact API

| Method | Endpoint | Provider |
| --- | --- | --- |
| GET | `/salesforce/contacts` | Salesforce |
| GET | `/hubspot/contacts` | HubSpot |

Example requests:

```bash
curl -i http://localhost:8080/salesforce/contacts
curl -i http://localhost:8080/hubspot/contacts
```

These endpoints are read-only. They retrieve contacts without importing them into Record Service or modifying either CRM.

### Response format

A successful request returns HTTP `200` with a `ContactRetrievalResponse` object:

```json
{
  "contacts": [
    {
      "source": "SALESFORCE",
      "externalId": "003000000000001",
      "firstName": "Maria",
      "lastName": "Johnson",
      "email": "maria@example.com"
    }
  ],
  "totalAvailable": 251,
  "hasMore": true,
  "continuationToken": "/services/data/v66.0/query/example-200"
}
```

The continuation token above is illustrative. Callers must use the value supplied by the provider rather than constructing one.

| Field | Meaning |
| --- | --- |
| `contacts` | Normalized contacts returned in this retrieval |
| `totalAvailable` | Total matching contacts reported by the provider, or `null` when unavailable |
| `hasMore` | Whether the provider response indicates another page |
| `continuationToken` | Provider continuation value, or `null` when no further page is indicated |

`totalAvailable` is independent of the number of contacts in the current page.

For HubSpot, the response uses the same structure, but `totalAvailable` is `null`. When another page exists, `continuationToken` contains HubSpot's `paging.next.after` value.

An empty completed Salesforce retrieval is represented as:

```json
{
  "contacts": [],
  "totalAvailable": 0,
  "hasMore": false,
  "continuationToken": null
}
```

For an empty completed HubSpot retrieval, `totalAvailable` remains `null`.

### Contact fields

| Field | JSON type | Meaning |
| --- | --- | --- |
| `source` | string | `SALESFORCE` or `HUBSPOT` |
| `externalId` | string | Contact ID supplied by the CRM |
| `firstName` | string or null | First name supplied by the CRM |
| `lastName` | string or null | Last name supplied by the CRM |
| `email` | string or null | Email supplied by the CRM |

Provider IDs are preserved as text. Missing or explicitly null names and email addresses remain null.

The mapper does not trim, validate, merge, or match contacts by name or email. It does not assign local contact IDs.

A missing/null provider results array or a missing HubSpot `properties` object is treated as malformed input rather than an empty successful result.

### Pagination and retrieval limits

Each request retrieves one provider page. A successful response does not imply that every matching CRM contact was returned.

| Provider | Retrieval behavior |
| --- | --- |
| Salesforce | Queries Contact fields through REST API `v66.0` and requests `batchSize=200` using `Sforce-Query-Options` |
| HubSpot | Requests the Contacts API with `limit=10` |

Salesforce may return fewer records than the requested batch size. Use the returned metadata to determine whether another page exists.

Provider metadata is mapped as follows:

| Shared field | Salesforce | HubSpot |
| --- | --- | --- |
| `contacts` | Mapped `records` | Mapped `results` |
| `totalAvailable` | `totalSize` | `null` |
| `hasMore` | Inverse of `done` | Whether `paging.next` exists |
| `continuationToken` | `nextRecordsUrl` | `paging.next.after` |

The connectors do not follow continuation values automatically. The local endpoints do not yet accept a continuation token.

Record ordering is not guaranteed by the local API. Full traversal and recovery are separate work tracked under KAN-51.

### Authentication recovery and errors

Access tokens are obtained and cached on the server.

| Provider | Authentication recovery |
| --- | --- |
| Salesforce | HTTP `401` with the first error's `errorCode` equal to `INVALID_SESSION_ID` triggers a new JWT token exchange and one retry |
| HubSpot | HTTP `401` with `category` equal to `EXPIRED_AUTHENTICATION` triggers a refresh-token exchange and one retry |

The retry retains the retrieval endpoint and its configured limits. Salesforce also retains its batch-size header.

If token replacement fails, the operation stops before another contact request. A failed retry is not retried again.

Both connectors check the final response status before mapping contacts. A final non-`200` response raises an exception rather than being treated as a successful retrieval.

There is no general retry loop for rate limits, network failures, or other provider errors. Interrupted HTTP operations restore the thread's interrupted flag and raise an exception.

Unhandled failures use Spring Boot's default error handling and normally produce HTTP `500`. Provider status codes are not forwarded directly, and the application does not yet define a stable error-response DTO.

## Tests

Run the full suite:

### Linux/macOS

```bash
./mvnw test
```

### Windows PowerShell

```powershell
.\mvnw.cmd test
```

Connector tests use mocked HTTP responses and real JSON mapping, without live CRM requests. The Spring context test supplies dummy HubSpot configuration.

Relevant test areas include:

- Contact mapping and response serialization.
- Empty successful retrievals.
- Responses with and without another page.
- Nullable provider totals and continuation tokens.
- Retrieval limits retained during authentication retries.
- Provider errors and unsuccessful authentication retries.

## Docker

Build the application image:

```bash
docker build -t crm-integration-service .
```

The image exposes port `8080`. Running it requires the environment variables and Salesforce signing files described above, at the paths expected inside the container.

The Docker build uses `-DskipTests`; run the test suite separately.

## Code organization

- Provider controllers expose the local retrieval endpoints.
- `crm` contains shared contact and authentication contracts.
- `crm.response` contains the shared retrieval response.
- Provider connectors perform CRM HTTP requests.
- Token providers obtain and cache access tokens.
- Provider model and response packages represent external JSON structures.
- Provider mappers normalize contacts and retrieval metadata.
- `config` provides shared HTTP client and clock configuration.

CRM writes, automatic synchronization, full pagination, Kafka messaging, and frontend applications remain separate capabilities.