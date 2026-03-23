-- =============================================================================
-- Federated Attribute Repository — consolidated schema
-- =============================================================================

-- Schemas (must precede certificates for FK)
CREATE TABLE schemas
(
    id          UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    namespace   VARCHAR(255)             NOT NULL,
    name        VARCHAR(255)             NOT NULL,
    description TEXT,
    version     INT                      NOT NULL DEFAULT 1,
    fields      JSONB                    NOT NULL DEFAULT '[]',
    active      BOOLEAN                  NOT NULL DEFAULT TRUE,
    owner       VARCHAR(255),
    created     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    modified    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (namespace, name)
);

CREATE INDEX idx_schemas_namespace ON schemas (namespace);
CREATE INDEX idx_schemas_active ON schemas (active);

-- Certificates
CREATE TABLE certificates
(
    id                  UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    namespace           VARCHAR(255)             NOT NULL,
    identifier          VARCHAR(255)             NOT NULL,
    urn                 VARCHAR(512)             NOT NULL UNIQUE,
    attributes          JSONB                             DEFAULT '{}',
    status              VARCHAR(50)              NOT NULL DEFAULT 'DRAFT',
    integrity_digest    VARCHAR(512),
    integrity_algorithm VARCHAR(50),
    owner               VARCHAR(255),
    schema_id           UUID REFERENCES schemas (id),
    schema_version      INT,
    version             INTEGER                  NOT NULL DEFAULT 1,
    created             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    modified            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_certificates_namespace ON certificates (namespace);
CREATE INDEX idx_certificates_status ON certificates (status);
CREATE INDEX idx_certificates_owner ON certificates (owner);
CREATE INDEX idx_certificates_modified ON certificates (modified DESC);

-- Documents
CREATE TABLE documents
(
    id        UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    filename  VARCHAR(512)             NOT NULL,
    media     VARCHAR(255),
    size      BIGINT                   NOT NULL DEFAULT 0,
    digest    VARCHAR(512),
    signature TEXT,
    uploader  VARCHAR(255),
    uploaded  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE document_content
(
    id      UUID PRIMARY KEY REFERENCES documents (id) ON DELETE CASCADE,
    content BYTEA NOT NULL
);

CREATE TABLE certificate_documents
(
    certificate_id UUID                     NOT NULL REFERENCES certificates (id) ON DELETE CASCADE,
    document_id    UUID                     NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    attached       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (certificate_id, document_id)
);

CREATE INDEX idx_certificate_documents_document ON certificate_documents (document_id);

-- Peers
CREATE TABLE peers
(
    identity   VARCHAR(512) PRIMARY KEY,
    endpoint   VARCHAR(512)             NOT NULL,
    namespaces JSONB                    NOT NULL DEFAULT '[]',
    key        TEXT,
    key_id     VARCHAR(255),
    previous   JSONB                    NOT NULL DEFAULT '[]',
    seen       TIMESTAMP WITH TIME ZONE,
    priority   INTEGER                  NOT NULL DEFAULT 2147483647,
    enabled    BOOLEAN                  NOT NULL DEFAULT TRUE,
    base       VARCHAR(512),
    depth      INTEGER                  NOT NULL DEFAULT 5,
    created    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Schema versions
CREATE TABLE schema_versions
(
    schema_id   UUID                     NOT NULL REFERENCES schemas (id) ON DELETE CASCADE,
    version     INT                      NOT NULL,
    description TEXT,
    fields      JSONB                    NOT NULL DEFAULT '[]',
    active      BOOLEAN                  NOT NULL DEFAULT TRUE,
    created     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (schema_id, version)
);

CREATE INDEX idx_schema_versions_schema ON schema_versions (schema_id);

-- Field-level access policies
CREATE TABLE field_policies
(
    schema_id UUID         NOT NULL REFERENCES schemas (id) ON DELETE CASCADE,
    field     VARCHAR(255) NOT NULL,
    kind      VARCHAR(50)  NOT NULL DEFAULT 'public',
    role      VARCHAR(255),
    PRIMARY KEY (schema_id, field)
);

CREATE INDEX idx_field_policies_schema ON field_policies (schema_id);

-- Certificate audit history
CREATE TABLE certificate_history
(
    id      UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    urn     VARCHAR(512)             NOT NULL REFERENCES certificates (urn) ON DELETE CASCADE,
    type    VARCHAR(50)              NOT NULL,
    actor   VARCHAR(255),
    details JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_history_urn ON certificate_history (urn);
