# Federated Attribute Repository

A certificate repository for the FAR ecosystem — the write-side counterpart to
the [Federated Attribute Resolver](../federated-attribute-resolver). Stores certificates with document bindings,
publishes metadata to ledgers, and implements the FAR Driver interface so the resolver can query it directly.

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+ (for the UI; installed automatically by `frontend-maven-plugin`)
- Docker (for PostgreSQL and Keycloak via Quarkus DevServices)
- The resolver project installed locally:

```bash
cd ../federated-attribute-resolver
mvn install -DskipTests
```

## Build

```bash
# Compile all modules (excluding UI)
mvn compile

# Compile everything including the Vue frontend
mvn compile -pl repository-ui

# Install to local Maven repo
mvn install -DskipTests
```

## Development

```bash
mvn quarkus:dev -pl repository-server
```

This single command starts the backend on port 8081 with everything provisioned automatically via Quarkus DevServices:

| Service        | Port       | Provisioned by                                         |
|----------------|------------|--------------------------------------------------------|
| Repository API | 8081       | Quarkus                                                |
| PostgreSQL     | _(random)_ | DevServices — auto-created, schema migrated via Flyway |
| Keycloak       | 8180       | DevServices — preconfigured from `dev-realm.json`      |

No manual database or identity provider setup is needed. Docker must be running.

### Dev Users

The dev realm (`dev-realm.json`) is imported into Keycloak automatically and includes two test users:

| User    | Password | Roles              | Purpose                                               |
|---------|----------|--------------------|-------------------------------------------------------|
| `alice` | `alice`  | `repository-admin` | Admin — can publish ledger entries                    |
| `bob`   | `bob`    | _(none)_           | Regular user — can create and manage own certificates |

Two OIDC clients are preconfigured:

| Client          | Type                            | Purpose                                 |
|-----------------|---------------------------------|-----------------------------------------|
| `repository`    | confidential (secret: `secret`) | Backend token validation                |
| `repository-ui` | public                          | Frontend Authorization Code + PKCE flow |

### Vue Dev Server

To work on the frontend with hot reload, start the Vite dev server in a separate terminal:

```bash
cd repository-ui && npm run dev
```

The Vite dev server runs on port 5173 and proxies `/v1` requests to the backend at 8081. The OIDC defaults in `auth.ts`
already point at `http://localhost:8180/realms/far`, matching the DevServices Keycloak.

### Quarkus Dev UI

Quarkus provides a built-in dev UI at `http://localhost:8081/q/dev-ui` with live configuration, extension status, and
the Keycloak admin console link.

## Production

Build the UI first — its output is packaged into `repository-server` as static resources under `META-INF/resources/`:

```bash
mvn package -DskipTests
java -jar repository-server/target/quarkus-app/quarkus-run.jar
```

In production, DevServices is disabled. You must provide an external OIDC provider and PostgreSQL.
See [Backend Configuration](#backend-configuration) for required environment variables.

## Test

```bash
# Unit tests (no Docker required)
mvn test -pl repository-model,repository-spi,repository-core,repository-rest,repository-driver,repository-ledger-bitcoin

# Integration tests (requires Docker for PostgreSQL)
mvn test -pl repository-test-integration

# Single test class
mvn test -pl repository-test-integration -Dtest=AttributeSearchIntegrationTests

# All tests
mvn test
```

Unit tests cover domain models, filter parsing, schema validation, field policy application, hashing, and the repository
driver. Integration tests cover the full REST API including certificate CRUD, attribute search, schema management,
document handling, ledger publishing, federated resolution, and field-level security policies.

## Authentication & Authorization

The repository uses OpenID Connect (OIDC) for authentication and role-based access control for authorization. Any
standards-compliant OIDC provider works — Keycloak, Auth0, Entra ID, Okta, etc.

### OIDC Provider Setup

Register two clients with your OIDC provider:

| Client          | Type         | Flow                                   | Purpose                                    |
|-----------------|--------------|----------------------------------------|--------------------------------------------|
| `repository`    | confidential | Client Credentials / Bearer validation | Backend API — validates access tokens      |
| `repository-ui` | public       | Authorization Code + PKCE              | Frontend SPA — obtains tokens for the user |

Create a realm role named `repository-admin` and assign it to users who should be able to publish ledger entries.

### Backend Configuration

The backend validates Bearer tokens on every API request (except public endpoints). In production, configure via
environment variables:

| Environment Variable   | Description                                                                                      |
|------------------------|--------------------------------------------------------------------------------------------------|
| `OIDC_AUTH_SERVER_URL` | OIDC discovery endpoint (e.g. `https://idp.example.com/realms/far`) — **required in production** |
| `OIDC_CLIENT_ID`       | Backend client ID (default: `repository`)                                                        |
| `OIDC_CLIENT_SECRET`   | Backend client secret (default: `secret`)                                                        |

In dev mode these are not needed — DevServices provides Keycloak automatically.

### Frontend Configuration

The Vue UI uses the Authorization Code flow with PKCE — no client secret is needed. Configure via `.env` or environment
variables:

| Environment Variable  | Default                            | Description                                        |
|-----------------------|------------------------------------|----------------------------------------------------|
| `VITE_OIDC_AUTHORITY` | `http://localhost:8180/realms/far` | OIDC discovery endpoint (same provider as backend) |
| `VITE_OIDC_CLIENT_ID` | `repository-ui`                    | Public client ID registered with the provider      |

The UI redirects unauthenticated users to the OIDC login page. After authentication, the provider redirects back to
`/callback`, and the UI attaches the access token as a `Bearer` header on all API requests. Token renewal is automatic.

### Authorization Model

Security is enforced at the resource level with JAX-RS annotations (`@Authenticated`, `@RolesAllowed`, `@PermitAll`).
Certificate ownership is enforced programmatically — on mutating operations, the backend checks that the authenticated
principal matches the certificate owner (or holds the `repository-admin` role).

| Endpoint                             | Method            | Access                               |
|--------------------------------------|-------------------|--------------------------------------|
| `/v1/health`                         | GET               | public                               |
| `/v1/auth`                           | GET               | public                               |
| `/v1/certificates`                   | GET               | authenticated                        |
| `/v1/certificates`                   | POST              | authenticated (owner set from token) |
| `/v1/certificates/{urn}`             | GET, HEAD         | authenticated                        |
| `/v1/certificates/{urn}`             | PUT, DELETE       | owner or `repository-admin`          |
| `/v1/certificates/{urn}/documents`   | GET, POST, DELETE | owner or `repository-admin`          |
| `/v1/certificates/{urn}/entries`     | GET               | authenticated                        |
| `/v1/certificates/{urn}/entries`     | POST              | `repository-admin`                   |
| `/v1/documents`                      | POST              | authenticated (upload)               |
| `/v1/documents`                      | GET               | `repository-admin`                   |
| `/v1/documents/{id}`                 | GET               | owner or `repository-admin`          |
| `/v1/documents/{id}/metadata`        | GET               | owner or `repository-admin`          |
| `/v1/schemas`                        | GET               | authenticated                        |
| `/v1/schemas`                        | POST              | authenticated (owner set from token) |
| `/v1/schemas/{id}`                   | GET               | authenticated                        |
| `/v1/schemas/{id}`                   | PUT               | owner or `repository-admin`          |
| `/v1/schemas/{id}/versions`          | GET               | authenticated                        |
| `/v1/schemas/{id}/versions/{v}`      | GET               | authenticated                        |
| `/v1/schemas/{id}/policies`          | GET               | authenticated                        |
| `/v1/schemas/{id}/policies`          | PUT               | owner or `repository-admin`          |
| `/v1/namespaces`                     | GET               | public                               |
| `/v1/namespaces/{ns}`                | GET               | public                               |
| `/v1/peers`                          | GET               | public                               |
| `/v1/peers/configuration`            | GET               | public                               |
| `/v1/peers`                          | POST              | `repository-admin`                   |
| `/v1/peers/{identity}`               | DELETE            | `repository-admin`                   |
| `/v1/resources`                      | GET               | public (federated search)            |
| `/v1/resources/{urn}`                | GET, HEAD         | public (federated resolve)           |
| `/v1/resources/{urn}/history`        | GET               | public                               |
| `/v1/resources/{urn}/documents`      | GET               | authenticated                        |
| `/v1/resources/{urn}/documents/{id}` | GET               | authenticated                        |

**Owner assignment:** When a certificate is created, the `owner` field is set to the authenticated user's principal
name (from the access token), regardless of what the request body contains.

**Ownership enforcement:** PUT and DELETE on a certificate require the caller to be either the certificate's owner or
hold the `repository-admin` role. Violations return `403 Forbidden`.

### Testing

Integration tests bypass OIDC entirely using Quarkus `@TestSecurity` annotations, which inject a mock identity without
requiring a running provider:

```java
@QuarkusTest
@TestSecurity(user = "tester")
class CertificateIntegrationTest { ... }

@QuarkusTest
@TestSecurity(user = "tester", roles = {"repository-admin"})
class LedgerIntegrationTest { ... }
```

OIDC is disabled in the test profile (`quarkus.oidc.enabled=false`).

## Search & Filtering

The certificate search endpoint (`GET /v1/certificates`) supports an OData-style `$filter` query parameter with
pagination (`$top`, `$skip`) and sorting (`$orderby`).

**Supported operators:** `eq`, `ne`, `gt`, `ge`, `lt`, `le`, `in`, `contains()`

**Built-in fields:** `identifier`, `namespace`, `urn`, `status`, `owner`

**Custom attributes:** Any attribute stored in the certificate's JSONB `attributes` column can be filtered — the store
queries `jsonb_extract_path_text()` at the SQL level.

**Examples:**

```
GET /v1/certificates?$filter=status eq 'ACTIVE'
GET /v1/certificates?$filter=namespace eq 'carbon' and contains(identifier, 'VCS')
GET /v1/certificates?$filter=region eq 'EU' and volume gt 500
GET /v1/certificates?$filter=vintage eq '2024'&$orderby=modified desc&$top=10
```

The federated search endpoint (`GET /v1/resources`) uses the same filter syntax and queries across both local and peer
registries.

The UI provides both a simple certificate list with client-side filtering and an advanced search view with a predicate
builder. The search dropdown includes Identifier, URN, and Certificate Type (namespace), with an "Other..." option for
searching by arbitrary custom attributes.

## Schemas & Field Policies

Schemas define the expected structure of a certificate's attributes within a namespace. Each schema has versioned
revisions and field-level access policies.

**Field policies** control attribute visibility:

- **Public** — visible to all authenticated users
- **Masked** — value replaced with `***` for non-admin users
- **Credential** — visible only to users with a specific role; omitted entirely otherwise

Admin users (`repository-admin`) bypass all field policies.

## Federation

Registries can peer with each other to form a federated network. Each repository owns a set of namespaces and delegates
queries for unknown namespaces to its peers.

**Peer discovery:** `GET /v1/peers/configuration` returns the repository's identity, namespaces, public key, and
endpoint information. Adding a peer (`POST /v1/peers`) fetches this configuration automatically.

**Delegation:** When a `GET /v1/resources` query includes namespaces not owned locally, the resolver fans out to
connected peers. The `Far-Delegation-Chain` header prevents infinite loops.

**Signatures:** Each repository signs responses with an Ed25519 key. Peer repositories validate signatures using the
public key exchanged during peering.

## Docker Multi-Repository

The `docker-compose.yaml` defines a five-repository federated network for local development and demos:

| Service     | Port | Namespaces         |
|-------------|------|--------------------|
| netherlands | 8081 | carbon, energy     |
| france      | 8082 | agriculture, trade |
| belgium     | 8083 | iso, supply-chain  |
| spain       | 8084 | renewable-energy   |
| cxl         | 8085 | ngg                |

All five repositories share a single PostgreSQL (separate databases per repository) and a single Keycloak instance on
port 8180.

```bash
# Build and start all registries
docker compose up --build

# Start a single repository
docker compose up netherlands
```

### Docker Users

The Docker realm (`docker/realm.json`) includes four test users:

| User    | Password | Roles              |
|---------|----------|--------------------|
| `alice` | `alice`  | `repository-admin` |
| `bob`   | `bob`    | _(none)_           |
| `carol` | `carol`  | `repository-admin` |
| `dave`  | `dave`   | _(none)_           |

Additionally, `docker/realm-a.json` (realm `far-a`) has alice/bob for use with a two-repository split, and
`docker/realm-b.json` (realm `far-b`) has carol/dave.

## Configuration

Key properties in `repository-server/src/main/resources/application.yaml`:

| Property                      | Default                              | Description                                               |
|-------------------------------|--------------------------------------|-----------------------------------------------------------|
| `quarkus.http.port`           | 8081                                 | HTTP listen port                                          |
| `repository.identity`         | `https://repository.far.example.com` | Repository identity URI                                   |
| `repository.namespaces`       | `carbon,energy,...`                  | Comma-separated supported namespaces                      |
| `repository.keys.id`          | `key-1`                              | Ed25519 signing key identifier                            |
| `repository.keys.directory`   | `data/keys`                          | Path to PEM key files                                     |
| `repository.ssrf.check`       | `true`                               | Validate peer endpoints are not private addresses         |
| `repository.delegation.depth` | `5`                                  | Maximum federated delegation chain depth                  |
| `repository.protocol.version` | `0.1.0`                              | FAR protocol version                                      |
| `repository.oidc.authority`   | `http://localhost:8180/realms/far`   | OIDC authority URL (served to frontend via `/v1/auth`)    |
| `repository.oidc.client`      | `repository-ui`                      | Public OIDC client ID (served to frontend via `/v1/auth`) |
| `repository.csp.connect`      | `http://localhost:8180`              | Allowed CSP `connect-src` for OIDC provider               |

Production environment variables:

| Variable                    | Description                                                                  |
|-----------------------------|------------------------------------------------------------------------------|
| `OIDC_AUTH_SERVER_URL`      | OIDC provider URL (required)                                                 |
| `OIDC_TOKEN_ISSUER`         | Token issuer URL if different from auth server URL                           |
| `OIDC_TLS_VERIFICATION`     | TLS verification mode (default: `required`)                                  |
| `DB_URL`                    | PostgreSQL JDBC URL (default: `jdbc:postgresql://localhost:5432/repository`) |
| `DB_USER`                   | Database username (default: `repository`)                                    |
| `DB_PASSWORD`               | Database password (default: `repository`)                                    |
| `REPOSITORY_IDENTITY`       | Repository identity URI                                                      |
| `REPOSITORY_NAMESPACES`     | Comma-separated supported namespaces                                         |
| `REPOSITORY_SEED`           | Seed data profile name (default: `none`)                                     |
| `REPOSITORY_OIDC_AUTHORITY` | OIDC authority URL for frontend                                              |
| `REPOSITORY_OIDC_CLIENT`    | Frontend client ID (default: `repository-ui`)                                |
| `REPOSITORY_CORS_ORIGINS`   | Allowed CORS origins                                                         |
| `REPOSITORY_SSRF_CHECK`     | Enable SSRF validation for peer endpoints                                    |
| `REPOSITORY_KEYS_DIR`       | Path to signing key files                                                    |
| `REPOSITORY_CSP_CONNECT`    | CSP `connect-src` for OIDC provider                                          |

## Architecture

```
repository-model          Domain records, enums, exceptions
repository-spi            Store + Ledger SPI interfaces
repository-store-db       JDBI persistence (PostgreSQL, JSONB)
repository-ledger-db      Database ledger implementation
repository-ledger-bitcoin Bitcoin OP_RETURN anchoring
repository-core           Business logic (Repository, Binder, Hasher, Filter)
repository-rest           JAX-RS resources + OData-style filter parser
repository-driver         Implements far-spi Driver interface
repository-server         Quarkus application (CDI wiring, config, seed data)
repository-ui             Vue 3 + TypeScript + Tailwind frontend
repository-test-integration  End-to-end tests
```

**Dependency flow:** model → spi → store-db / ledger-db / ledger-bitcoin → core → rest / driver → server
