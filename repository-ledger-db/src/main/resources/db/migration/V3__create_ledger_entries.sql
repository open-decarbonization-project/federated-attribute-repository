CREATE TABLE ledger_entries
(
    id        UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    urn       VARCHAR(512)             NOT NULL,
    ledger    VARCHAR(255)             NOT NULL,
    hash      VARCHAR(512)             NOT NULL,
    proof     TEXT,
    published TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_entries_urn ON ledger_entries (urn);
CREATE INDEX idx_ledger_entries_ledger ON ledger_entries (ledger);
