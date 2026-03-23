-- Spain seed data: renewable-energy namespace

-- Andalusia solar Guarantee of Origin
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0030-7000-8000-000000000030', 'renewable-energy', 'GO-SOLAR-ES-2024-Q2',
        'urn:far:renewable-energy:GO-SOLAR-ES-2024-Q2',
        '{"facility":{"name":"facility","value":"Planta Solar La Serena","source":"cnmc","verified":true},"source":{"name":"source","value":"Solar Photovoltaic","source":"cnmc","verified":true},"generation":{"name":"generation","value":{"amount":28500,"unit":"MWh"},"source":"cnmc","verified":true},"period":{"name":"period","value":"2024-Q2","source":"cnmc","verified":true},"location":{"name":"location","value":"Andalusia, Spain","source":"cnmc","verified":true},"issuer":{"name":"issuer","value":"CNMC","source":"cnmc","verified":true},"tracking":{"name":"tracking","value":"AIB EECS","source":"cnmc","verified":true},"commissioned":{"name":"commissioned","value":"2021-06-15T00:00:00Z","source":"cnmc","verified":true}}',
        'ACTIVE', 'sha256:e6f7a8b9c0d1', 'SHA-256', 'alice',
        '01912c6a-0009-7000-8000-000000000009', now() - interval '40 days', now() - interval '5 days');

-- Galicia wind Guarantee of Origin
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0031-7000-8000-000000000031', 'renewable-energy', 'GO-WIND-ES-2024-Q3',
        'urn:far:renewable-energy:GO-WIND-ES-2024-Q3',
        '{"facility":{"name":"facility","value":"Parque Eólico Serra do Xistral","source":"cnmc","verified":true},"source":{"name":"source","value":"Onshore Wind","source":"cnmc","verified":true},"generation":{"name":"generation","value":{"amount":35200,"unit":"MWh"},"source":"cnmc","verified":true},"period":{"name":"period","value":"2024-Q3","source":"cnmc","verified":true},"location":{"name":"location","value":"Galicia, Spain","source":"cnmc","verified":true},"issuer":{"name":"issuer","value":"CNMC","source":"cnmc","verified":true},"tracking":{"name":"tracking","value":"AIB EECS","source":"cnmc","verified":true},"commissioned":{"name":"commissioned","value":"2019-11-20T00:00:00Z","source":"cnmc","verified":true}}',
        'ACTIVE', 'sha256:f7a8b9c0d1e2', 'SHA-256', 'carol',
        '01912c6a-0009-7000-8000-000000000009', now() - interval '25 days', now() - interval '3 days');

-- Castilla-La Mancha solar Guarantee of Origin
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0032-7000-8000-000000000032', 'renewable-energy', 'GO-SOLAR-ES-2024-Q4',
        'urn:far:renewable-energy:GO-SOLAR-ES-2024-Q4',
        '{"facility":{"name":"facility","value":"Planta Fotovoltaica Núñez de Balboa","source":"cnmc","verified":true},"source":{"name":"source","value":"Solar Photovoltaic","source":"cnmc","verified":true},"generation":{"name":"generation","value":{"amount":52000,"unit":"MWh"},"source":"cnmc","verified":true},"period":{"name":"period","value":"2024-Q4","source":"cnmc","verified":true},"location":{"name":"location","value":"Castilla-La Mancha, Spain","source":"cnmc","verified":true},"issuer":{"name":"issuer","value":"CNMC","source":"cnmc","verified":true},"tracking":{"name":"tracking","value":"AIB EECS","source":"cnmc","verified":true},"commissioned":{"name":"commissioned","value":"2020-03-01T00:00:00Z","source":"cnmc","verified":true}}',
        'ACTIVE', 'sha256:a8b9c0d1e2f3', 'SHA-256', 'bob',
        '01912c6a-0009-7000-8000-000000000009', now() - interval '10 days', now() - interval '1 day');

-- Ledger entries for Spain
INSERT INTO ledger_entries (id, urn, ledger, hash, published)
VALUES ('01912c6c-0030-7000-8000-000000000030', 'urn:far:renewable-energy:GO-SOLAR-ES-2024-Q2', 'database',
        'sha-256=:ZTZmN2E4YjljMGQxZTZmN2E4YjljMGQxZTZmN2E4Yjk=:', now() - interval '35 days'),
       ('01912c6c-0031-7000-8000-000000000031', 'urn:far:renewable-energy:GO-WIND-ES-2024-Q3', 'database',
        'sha-256=:ZjdhOGI5YzBkMWUyZjdhOGI5YzBkMWUyZjdhOGI5YzA=:', now() - interval '22 days'),
       ('01912c6c-0032-7000-8000-000000000032', 'urn:far:renewable-energy:GO-SOLAR-ES-2024-Q4', 'database',
        'sha-256=:YThiOWMwZDFlMmYzYThiOWMwZDFlMmYzYThiOWMwZDE=:', now() - interval '8 days');
