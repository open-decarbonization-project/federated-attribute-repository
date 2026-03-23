-- Belgium seed data: iso + supply-chain namespaces

-- ISO 14001 (reused from old seed-b)
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0003-7000-8000-000000000003', 'iso', 'EMS-2024-ACME-001',
        'urn:far:iso:EMS-2024-ACME-001',
        '{"organization":{"name":"organization","value":"Acme Manufacturing Ltd","source":"bsi","verified":true},"scope":{"name":"scope","value":"Design, manufacture, and distribution of industrial components","source":"bsi","verified":true},"body":{"name":"body","value":"BSI Group","source":"bsi","verified":true},"issued":{"name":"issued","value":"2024-03-15T00:00:00Z","source":"bsi","verified":true},"expiry":{"name":"expiry","value":"2027-03-14T23:59:59Z","source":"bsi","verified":true},"accreditation":{"name":"accreditation","value":"UKAS 003","source":"bsi","verified":true}}',
        'ACTIVE', 'sha256:c3d4e5f6a1b2', 'SHA-256', 'carol',
        '01912c6a-0002-7000-8000-000000000002', now() - interval '60 days', now() - interval '10 days');

-- Supply chain provenance (reused from old seed-b)
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0007-7000-8000-000000000007', 'supply-chain', 'PROV-LI-BAT-2024-0892',
        'urn:far:supply-chain:PROV-LI-BAT-2024-0892',
        '{"product":{"name":"product","value":"Lithium-Ion Battery Pack (75 kWh)","source":"manufacturer","verified":true},"manufacturer":{"name":"manufacturer","value":"Northvolt AB","source":"manufacturer","verified":true},"origin":{"name":"origin","value":"Sweden","source":"manufacturer","verified":true},"batch":{"name":"batch","value":"NV-2024-Q3-0892","source":"manufacturer","verified":true},"quantity":{"name":"quantity","value":500,"source":"manufacturer","verified":true},"manufactured":{"name":"manufactured","value":"2024-08-22T00:00:00Z","source":"manufacturer","verified":true}}',
        'ACTIVE', 'sha256:a1c2e3b4d5f6', 'SHA-256', 'dave',
        '01912c6a-0006-7000-8000-000000000006', now() - interval '10 days', now() - interval '1 day');

-- ISO 9001 Volvo quality management
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0022-7000-8000-000000000022', 'iso', 'QMS-2024-VOLVO-003',
        'urn:far:iso:QMS-2024-VOLVO-003',
        '{"organization":{"name":"organization","value":"Volvo Cars Corporation","source":"dnv","verified":true},"scope":{"name":"scope","value":"Design, development, and manufacturing of passenger vehicles","source":"dnv","verified":true},"body":{"name":"body","value":"DNV GL","source":"dnv","verified":true},"issued":{"name":"issued","value":"2024-02-01T00:00:00Z","source":"dnv","verified":true},"expiry":{"name":"expiry","value":"2027-01-31T23:59:59Z","source":"dnv","verified":true},"accreditation":{"name":"accreditation","value":"SWEDAC 1002","source":"dnv","verified":true}}',
        'ACTIVE', 'sha256:c4d5e6f7a8b9', 'SHA-256', 'alice',
        '01912c6a-0002-7000-8000-000000000002', now() - interval '45 days', now() - interval '8 days');

-- EV motor provenance
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0023-7000-8000-000000000023', 'supply-chain', 'PROV-EV-MOT-2024-1547',
        'urn:far:supply-chain:PROV-EV-MOT-2024-1547',
        '{"product":{"name":"product","value":"Permanent Magnet Synchronous Motor (150 kW)","source":"manufacturer","verified":true},"manufacturer":{"name":"manufacturer","value":"Valeo Siemens eAutomotive","source":"manufacturer","verified":true},"origin":{"name":"origin","value":"Germany","source":"manufacturer","verified":true},"batch":{"name":"batch","value":"VSE-2024-Q2-1547","source":"manufacturer","verified":true},"quantity":{"name":"quantity","value":1200,"source":"manufacturer","verified":true},"manufactured":{"name":"manufactured","value":"2024-06-10T00:00:00Z","source":"manufacturer","verified":true}}',
        'ACTIVE', 'sha256:d5e6f7a8b9c0', 'SHA-256', 'bob',
        '01912c6a-0006-7000-8000-000000000006', now() - interval '20 days', now() - interval '3 days');

-- Ledger entries for Belgium
INSERT INTO ledger_entries (id, urn, ledger, hash, published)
VALUES ('01912c6c-0003-7000-8000-000000000003', 'urn:far:iso:EMS-2024-ACME-001', 'database',
        'sha-256=:YjRjNWQ2ZThiOWYwYTJjN2Q0ZTVmNmExYjJjM2Q0ZTU=:', now() - interval '55 days'),
       ('01912c6c-0007-7000-8000-000000000007', 'urn:far:supply-chain:PROV-LI-BAT-2024-0892', 'database',
        'sha-256=:YTFjMmUzYjRkNWY2YTFjMmUzYjRkNWY2YTFjMmUzYjQ=:', now() - interval '8 days'),
       ('01912c6c-0022-7000-8000-000000000022', 'urn:far:iso:QMS-2024-VOLVO-003', 'database',
        'sha-256=:YzRkNWU2ZjdhOGI5YzRkNWU2ZjdhOGI5YzRkNWU2Zjc=:', now() - interval '40 days'),
       ('01912c6c-0023-7000-8000-000000000023', 'urn:far:supply-chain:PROV-EV-MOT-2024-1547', 'database',
        'sha-256=:ZDVlNmY3YThiOWMwZDVlNmY3YThiOWMwZDVlNmY3YTg=:', now() - interval '18 days');
