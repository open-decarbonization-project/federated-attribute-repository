-- CXL seed data: ngg namespace

-- Dutch biomethane
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0040-7000-8000-000000000040', 'ngg', 'NGG-BIO-NL-2024-001',
        'urn:far:ngg:NGG-BIO-NL-2024-001',
        '{"facility":{"name":"facility","value":"Groningen Biogas Plant","source":"vertogas","verified":true},"operator":{"name":"operator","value":"Engie Energie Nederland","source":"vertogas","verified":true},"feedstock":{"name":"feedstock","value":"Agricultural waste and manure","source":"vertogas","verified":true},"grade":{"name":"grade","value":"A+","source":"vertogas","verified":true},"intensity":{"name":"intensity","value":{"amount":12.5,"unit":"gCO2/kWh"},"source":"vertogas","verified":true},"volume":{"name":"volume","value":{"amount":8500,"unit":"MWh"},"source":"vertogas","verified":true},"auditor":{"name":"auditor","value":"KIWA Technology","source":"vertogas","verified":true},"issued":{"name":"issued","value":"2024-05-01T00:00:00Z","source":"vertogas","verified":true},"expiry":{"name":"expiry","value":"2025-04-30T23:59:59Z","source":"vertogas","verified":true}}',
        'ACTIVE', 'sha256:b9c0d1e2f3a4', 'SHA-256', 'alice',
        '01912c6a-0008-7000-8000-000000000008', now() - interval '50 days', now() - interval '7 days');

-- German green hydrogen
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0041-7000-8000-000000000041', 'ngg', 'NGG-H2-DE-2024-042',
        'urn:far:ngg:NGG-H2-DE-2024-042',
        '{"facility":{"name":"facility","value":"Wesseling Green Hydrogen Hub","source":"dena","verified":true},"operator":{"name":"operator","value":"Shell Deutschland GmbH","source":"dena","verified":true},"feedstock":{"name":"feedstock","value":"Electrolysis (wind-powered)","source":"dena","verified":true},"grade":{"name":"grade","value":"A","source":"dena","verified":true},"intensity":{"name":"intensity","value":{"amount":3.8,"unit":"gCO2/kWh"},"source":"dena","verified":true},"volume":{"name":"volume","value":{"amount":4200,"unit":"MWh"},"source":"dena","verified":true},"auditor":{"name":"auditor","value":"TÜV SÜD","source":"dena","verified":true},"issued":{"name":"issued","value":"2024-07-15T00:00:00Z","source":"dena","verified":true},"expiry":{"name":"expiry","value":"2025-07-14T23:59:59Z","source":"dena","verified":true}}',
        'ACTIVE', 'sha256:c0d1e2f3a4b5', 'SHA-256', 'carol',
        '01912c6a-0008-7000-8000-000000000008', now() - interval '35 days', now() - interval '4 days');

-- French synthetic natural gas
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0042-7000-8000-000000000042', 'ngg', 'NGG-SNG-FR-2024-018',
        'urn:far:ngg:NGG-SNG-FR-2024-018',
        '{"facility":{"name":"facility","value":"Port-Jérôme Methanation Plant","source":"grdf","verified":true},"operator":{"name":"operator","value":"Storengy SA","source":"grdf","verified":true},"feedstock":{"name":"feedstock","value":"CO2 capture + green hydrogen","source":"grdf","verified":true},"grade":{"name":"grade","value":"B+","source":"grdf","verified":true},"intensity":{"name":"intensity","value":{"amount":28.3,"unit":"gCO2/kWh"},"source":"grdf","verified":true},"volume":{"name":"volume","value":{"amount":3100,"unit":"MWh"},"source":"grdf","verified":true},"auditor":{"name":"auditor","value":"Bureau Veritas","source":"grdf","verified":true},"issued":{"name":"issued","value":"2024-09-10T00:00:00Z","source":"grdf","verified":true},"expiry":{"name":"expiry","value":"2025-09-09T23:59:59Z","source":"grdf","verified":true}}',
        'ACTIVE', 'sha256:d1e2f3a4b5c6', 'SHA-256', 'dave',
        '01912c6a-0008-7000-8000-000000000008', now() - interval '20 days', now() - interval '2 days');

-- Ledger entries for CXL
INSERT INTO ledger_entries (id, urn, ledger, hash, published)
VALUES ('01912c6c-0040-7000-8000-000000000040', 'urn:far:ngg:NGG-BIO-NL-2024-001', 'database',
        'sha-256=:YjljMGQxZTJmM2E0YjljMGQxZTJmM2E0YjljMGQxZTI=:', now() - interval '45 days'),
       ('01912c6c-0041-7000-8000-000000000041', 'urn:far:ngg:NGG-H2-DE-2024-042', 'database',
        'sha-256=:YzBkMWUyZjNhNGI1YzBkMWUyZjNhNGI1YzBkMWUyZjM=:', now() - interval '30 days'),
       ('01912c6c-0042-7000-8000-000000000042', 'urn:far:ngg:NGG-SNG-FR-2024-018', 'database',
        'sha-256=:ZDFlMmYzYTRiNWM2ZDFlMmYzYTRiNWM2ZDFlMmYzYTQ=:', now() - interval '17 days');
