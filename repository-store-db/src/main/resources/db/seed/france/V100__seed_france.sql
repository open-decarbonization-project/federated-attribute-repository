-- France seed data: agriculture + trade namespaces

-- Organic (reused from old seed-b)
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0005-7000-8000-000000000005', 'agriculture', 'ORG-CA-2024-1187',
        'urn:far:agriculture:ORG-CA-2024-1187',
        '{"operation":{"name":"operation","value":"Greenfield Valley Farms","source":"ccof","verified":true},"category":{"name":"category","value":"Crop","source":"ccof","verified":true},"area":{"name":"area","value":{"amount":240,"unit":"hectares"},"source":"ccof","verified":true},"certifier":{"name":"certifier","value":"CCOF Certification Services","source":"ccof","verified":true},"issued":{"name":"issued","value":"2024-01-10T00:00:00Z","source":"ccof","verified":true},"transitional":{"name":"transitional","value":false,"source":"ccof","verified":true}}',
        'ACTIVE', 'sha256:e5f6a1b2c3d4', 'SHA-256', 'carol',
        '01912c6a-0004-7000-8000-000000000004', now() - interval '90 days', now() - interval '15 days');

-- Fair trade (reused from old seed-b)
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0004-7000-8000-000000000004', 'trade', 'FT-COF-ETH-2024-038',
        'urn:far:trade:FT-COF-ETH-2024-038',
        '{"producer":{"name":"producer","value":"Sidamo Coffee Cooperative Union","source":"fairtrade-intl","verified":true},"product":{"name":"product","value":"Arabica Coffee (green bean)","source":"fairtrade-intl","verified":true},"origin":{"name":"origin","value":"Ethiopia","source":"fairtrade-intl","verified":true},"premium":{"name":"premium","value":{"amount":0.20,"unit":"USD/kg"},"source":"fairtrade-intl","verified":true},"workers":{"name":"workers","value":3400,"source":"fairtrade-intl","verified":true},"organic":{"name":"organic","value":true,"source":"fairtrade-intl","verified":true}}',
        'ACTIVE', 'sha256:d4e5f6a1b2c3', 'SHA-256', 'dave',
        '01912c6a-0003-7000-8000-000000000003', now() - interval '45 days', now() - interval '3 days');

-- French organic vineyard
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0020-7000-8000-000000000020', 'agriculture', 'BIO-FR-2024-0456',
        'urn:far:agriculture:BIO-FR-2024-0456',
        '{"operation":{"name":"operation","value":"Domaine de la Romanée","source":"ecocert","verified":true},"category":{"name":"category","value":"Crop","source":"ecocert","verified":true},"area":{"name":"area","value":{"amount":85,"unit":"hectares"},"source":"ecocert","verified":true},"certifier":{"name":"certifier","value":"Ecocert France","source":"ecocert","verified":true},"issued":{"name":"issued","value":"2024-04-20T00:00:00Z","source":"ecocert","verified":true},"transitional":{"name":"transitional","value":false,"source":"ecocert","verified":true}}',
        'ACTIVE', 'sha256:a2b3c4d5e6f7', 'SHA-256', 'alice',
        '01912c6a-0004-7000-8000-000000000004', now() - interval '60 days', now() - interval '10 days');

-- Fair trade cocoa from Ghana
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0021-7000-8000-000000000021', 'trade', 'FT-CHO-GH-2024-102',
        'urn:far:trade:FT-CHO-GH-2024-102',
        '{"producer":{"name":"producer","value":"Kuapa Kokoo Cooperative","source":"fairtrade-intl","verified":true},"product":{"name":"product","value":"Cocoa Beans","source":"fairtrade-intl","verified":true},"origin":{"name":"origin","value":"Ghana","source":"fairtrade-intl","verified":true},"premium":{"name":"premium","value":{"amount":0.24,"unit":"USD/kg"},"source":"fairtrade-intl","verified":true},"workers":{"name":"workers","value":8500,"source":"fairtrade-intl","verified":true},"organic":{"name":"organic","value":false,"source":"fairtrade-intl","verified":true}}',
        'ACTIVE', 'sha256:b3c4d5e6f7a8', 'SHA-256', 'carol',
        '01912c6a-0003-7000-8000-000000000003', now() - interval '30 days', now() - interval '5 days');

-- Ledger entries for France
INSERT INTO ledger_entries (id, urn, ledger, hash, published)
VALUES ('01912c6c-0005-7000-8000-000000000005', 'urn:far:agriculture:ORG-CA-2024-1187', 'database',
        'sha-256=:ZTVmNmExYjJjM2Q0ZTVmNmExYjJjM2Q0ZTVmNmExYjI=:', now() - interval '85 days'),
       ('01912c6c-0004-7000-8000-000000000004', 'urn:far:trade:FT-COF-ETH-2024-038', 'database',
        'sha-256=:ZDRlNWY2YTFiMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTE=:', now() - interval '42 days'),
       ('01912c6c-0020-7000-8000-000000000020', 'urn:far:agriculture:BIO-FR-2024-0456', 'database',
        'sha-256=:YTJiM2M0ZDVlNmY3YTJiM2M0ZDVlNmY3YTJiM2M0ZDU=:', now() - interval '55 days'),
       ('01912c6c-0021-7000-8000-000000000021', 'urn:far:trade:FT-CHO-GH-2024-102', 'database',
        'sha-256=:YjNjNGQ1ZTZmN2E4YjNjNGQ1ZTZmN2E4YjNjNGQ1ZTY=:', now() - interval '25 days');
