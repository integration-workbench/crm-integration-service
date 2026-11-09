# CRM Integration Service

A Java 21 / Spring Boot application that retrieves Contacts from Salesforce and HubSpot and exposes them through a shared JSON response format.

This repository currently contains the CRM retrieval application. Persistent local Contacts, frontend applications and Kafka synchronization are later project milestones.

## Requirements

- JDK 21.
- Network access to download Maven dependencies and, for live requests, reach the configured CRM accounts.
- Salesforce JWT bearer authentication configured for a dedicated development account.
- A HubSpot OAuth app and an existing refresh token with permission to read Contacts.

The Maven wrapper is included; a separate Maven installation is not required.

## Local configuration

Supply these environment variables to the process running the application, for example through the IntelliJ run configuration or your shell:

| Variable | Purpose |
| --- | --- |
| `SF_CONSUMER_KEY` | Salesforce OAuth application's consumer key/client ID. |
| `SF_USER` | Salesforce username used as the JWT subject. |
| `SF_LOGIN_URL` | Salesforce login base URL used for the token exchange and JWT audience, without a trailing slash. |
| `HS_CLIENT_ID` | HubSpot OAuth application's client ID. |
| `HS_CLIENT_SECRET` | HubSpot OAuth application's client secret. |
| `HS_REFRESH_TOKEN` | Refresh token obtained during the initial HubSpot OAuth authorization. |

The Salesforce provider also reads these files relative to the JVM's `user.home`:

- `.secrets/crm-integration-service/server.key` — RSA signing private key.
- `.secrets/crm-integration-service/server.crt` — matching public key/certificate material read by the provider.

Keep credentials and signing files outside the repository. The application does not automatically load a `.env` file.

HubSpot configuration is required when the Spring application context starts. Salesforce signing files are read when a Salesforce token is requested.

The Salesforce connector uses the `instance_url` returned by its OAuth token exchange to select the Contact API host.

## Run locally

From the repository root, after setting the required configuration:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell, use `.\mvnw.cmd spring-boot:run`.

The default local address is `http://localhost:8080`.

## Contact API

| Method | Local endpoint | Source |
| --- | --- | --- |
| GET | `/salesforce/contacts` | Salesforce Contact SOQL query. |
| GET | `/hubspot/contacts` | HubSpot Contacts API. |

Example requests:

```bash
curl -i -H 'Accept: application/json' http://localhost:8080/salesforce/contacts
curl -i -H 'Accept: application/json' http://localhost:8080/hubspot/contacts
```

A successful request returns HTTP `200` with a JSON array of normalized Contacts, not the original provider response wrapper:

```json
[
  {
    "source": "HUBSPOT",
    "externalId": "283634468816",
    "firstName": "Maria",
    "lastName": "Johnson",
    "email": "maria@example.com"
  }
]
```

The Salesforce endpoint returns the same fields with `"source": "SALESFORCE"` and the Salesforce Contact ID, for example `"003000000000001"`.

### Fields and missing values

| Field | JSON type | Meaning |
| --- | --- | --- |
| `source` | string | `HUBSPOT` or `SALESFORCE`, assigned by the provider mapper. |
| `externalId` | string | The Contact ID supplied by that CRM, preserved as text. |
| `firstName` | string or null | First name supplied by the CRM. |
| `lastName` | string or null | Last name supplied by the CRM. |
| `email` | string or null | Email supplied by the CRM. |

Missing or explicitly null name/email values remain null in the DTO. Existing strings are copied without trimming, validation or case normalization. Missing names/email are not replaced with empty strings.

For example, a Contact without an email is represented as:

```json
[
  {
    "source": "SALESFORCE",
    "externalId": "003000000000001",
    "firstName": "Maria",
    "lastName": "Johnson",
    "email": null
  }
]
```

A valid provider response containing an empty `records` or `results` array maps to:

```json
[]
```

A missing/null results array or a missing HubSpot `properties` object is a malformed response for the current mapper; it is not treated as an empty result.

The API preserves source identity. It does not assign a local Contact ID, combine the two providers' results, or match people by name or email.

### Paging and retrieval limits

**Each request returns only the first page supplied by the provider. A successful response does not imply that every Contact in the CRM was returned.**

- Salesforce queries `SELECT Id, FirstName, LastName, Email FROM Contact` using REST API `v66.0`. There is no explicit SOQL `LIMIT`, but the query can still be paged by Salesforce. The connector does not follow `nextRecordsUrl`.
- HubSpot requests `/crm/objects/2026-09/contacts` without an explicit `limit` or `after` parameter, so the provider's default page size applies. The connector does not follow HubSpot paging information.
- The local endpoints accept no pagination parameters and return a bare array. They do not expose continuation tokens, a total count or a completeness flag.
- Record ordering is not guaranteed by the local API.

Full import pagination and recovery are later work, tracked separately in KAN-39.

### Authentication recovery and errors

The application obtains and caches CRM access tokens on the server.

| Provider | Recovery behavior |
| --- | --- |
| Salesforce | HTTP `401` with the first error's `errorCode` equal to `INVALID_SESSION_ID` triggers a new JWT token exchange and one retry of the Contact GET. A final non-`200` status raises an `IllegalStateException`. |
| HubSpot | HTTP `401` with `category` equal to `EXPIRED_AUTHENTICATION` triggers a refresh-token exchange and one retry of the Contact GET. Other categories do not trigger that retry. |

A token replacement failure stops the operation before another Contact GET. There is no retry loop for repeated authentication failures, network errors, rate limits or general provider failures. Interrupted HTTP operations restore the thread's interrupted flag and raise an exception.

**Current HubSpot limitation:** after the optional retry, the connector does not yet reject all non-`200` statuses before deserializing the body as a query response. A provider error can therefore surface as a JSON parsing or mapping exception instead of an explicit HTTP-status exception. The final status guard remains follow-up work for KAN-48.

The controllers currently let failures propagate to Spring Boot's default error handling. An upstream CRM `401`, `403` or `500` is not forwarded as that same HTTP status; unhandled application failures normally produce HTTP `500`. There is no application-defined error DTO or stable error-body schema yet. Spring Boot's error response can include fields such as `timestamp`, `status`, `error` and `path`; callers should not depend on an exception message being included.

## Tests

Run the existing isolated CRM tests without live credentials:

```bash
./mvnw -Dtest=HubSpotConnectorTest,HubSpotTokenProviderTest,SalesforceConnectorTest test
```

These tests use mocked HTTP responses and real Jackson mapping. Current coverage includes successful Contact mapping, basic authentication recovery, HubSpot token caching/expiry and Salesforce failure/retry bounds.

To run the whole suite, including the Spring context startup test:

```bash
./mvnw test
```

The context test supplies dummy HubSpot values and does not make live CRM calls. On Windows, use `.\mvnw.cmd test`.

Missing optional fields, empty results and the HTTP serialization/error boundary still need dedicated verification; the existing test fixtures do not yet cover all of those cases. GitHub Actions runs the Maven test suite on pushes.

## Docker

A two-stage Dockerfile builds and packages the application:

```bash
docker build -t crm-integration-service .
```

The image exposes port `8080`. Runtime execution still needs the environment variables above and the Salesforce signing files at the paths expected by the JVM inside the container.

The Docker build currently uses `-DskipTests`; building an image is not a substitute for running the test suite.

## Code organization

- `crm.salesforce` and `crm.hubspot` controller classes — local HTTP endpoints.
- `crm` — shared Contact DTO, source enum and authentication contracts.
- `crm.salesforce` and `crm.hubspot` — provider connectors and token providers.
- Provider `model` and `response` packages — external JSON shapes.
- Provider `mapper` packages — conversion into `ContactDto`.
- `config` — shared HTTP client and clock configuration.

The application is currently a local, read-only proof of concept. Concurrent token coordination, durable Contact storage, CRM writes, Kafka messaging, frontend applications and deployment orchestration remain separate milestones.
