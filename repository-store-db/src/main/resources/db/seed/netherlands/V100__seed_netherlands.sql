-- Netherlands seed data: carbon + energy namespaces

-- Carbon credits
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0001-7000-8000-000000000001', 'carbon', 'VCS-2847-2024',
        'urn:far:carbon:VCS-2847-2024',
        '{"project":{"name":"project","value":"Rimba Raya Biodiversity Reserve","source":"verra","verified":true},"volume":{"name":"volume","value":{"amount":5000,"unit":"tCO2e"},"source":"verra","verified":true},"vintage":{"name":"vintage","value":"2024","source":"verra","verified":true},"methodology":{"name":"methodology","value":"VM0007 REDD+ Methodology","source":"verra","verified":true},"repository":{"name":"repository","value":"Verra VCS","source":"verra","verified":true},"serial":{"name":"serial","value":"VCS-2847-2024-001-5000","source":"verra","verified":true},"retired":{"name":"retired","value":false,"source":"verra","verified":true}}',
        'ACTIVE', 'sha256:a1b2c3d4e5f6', 'SHA-256', 'alice',
        '01912c6a-0001-7000-8000-000000000001', now() - interval '30 days', now() - interval '2 days');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0002-7000-8000-000000000002', 'carbon', 'GS-1192-2024',
        'urn:far:carbon:GS-1192-2024',
        '{"project":{"name":"project","value":"Bujagali Hydropower Project","source":"goldstandard","verified":true},"volume":{"name":"volume","value":{"amount":12000,"unit":"tCO2e"},"source":"goldstandard","verified":true},"vintage":{"name":"vintage","value":"2024","source":"goldstandard","verified":true},"methodology":{"name":"methodology","value":"CDM AMS-I.D Grid Connected Renewable","source":"goldstandard","verified":true},"repository":{"name":"repository","value":"Gold Standard","source":"goldstandard","verified":true},"retired":{"name":"retired","value":false,"source":"goldstandard","verified":true}}',
        'ACTIVE', 'sha256:b2c3d4e5f6a1', 'SHA-256', 'alice',
        '01912c6a-0001-7000-8000-000000000001', now() - interval '20 days', now() - interval '5 days');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0012-7000-8000-000000000012', 'carbon', 'VCS-3391-2024',
        'urn:far:carbon:VCS-3391-2024',
        '{"project":{"name":"project","value":"Rwanda Improved Cookstoves Programme","source":"verra","verified":true},"volume":{"name":"volume","value":{"amount":22000,"unit":"tCO2e"},"source":"verra","verified":true},"vintage":{"name":"vintage","value":"2024","source":"verra","verified":true},"methodology":{"name":"methodology","value":"VMR0006 Energy Efficiency and Fuel Switch","source":"verra","verified":true},"repository":{"name":"repository","value":"Verra VCS","source":"verra","verified":true},"serial":{"name":"serial","value":"VCS-3391-2024-001-22000","source":"verra","verified":true},"retired":{"name":"retired","value":false,"source":"verra","verified":true}}',
        'ACTIVE', 'sha256:f4a5b6c7d8e9', 'SHA-256', 'bob',
        '01912c6a-0001-7000-8000-000000000001', now() - interval '8 days', now() - interval '1 day');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0008-7000-8000-000000000008', 'carbon', 'VCS-1455-2023',
        'urn:far:carbon:VCS-1455-2023',
        '{"project":{"name":"project","value":"Katingan Peatland Restoration","source":"verra","verified":true},"volume":{"name":"volume","value":{"amount":7500,"unit":"tCO2e"},"source":"verra","verified":true},"vintage":{"name":"vintage","value":"2023","source":"verra","verified":true},"methodology":{"name":"methodology","value":"VM0004 Peatland Rewetting","source":"verra","verified":true},"repository":{"name":"repository","value":"Verra VCS","source":"verra","verified":true},"serial":{"name":"serial","value":"VCS-1455-2023-001-7500","source":"verra","verified":true},"retired":{"name":"retired","value":true,"source":"verra","verified":true}}',
        'RETIRED', 'sha256:b1d2f3a4c5e6', 'SHA-256', 'alice',
        '01912c6a-0001-7000-8000-000000000001', now() - interval '180 days', now() - interval '30 days');

-- Energy certificates
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0006-7000-8000-000000000006', 'energy', 'REC-SOLAR-TX-2024-Q3',
        'urn:far:energy:REC-SOLAR-TX-2024-Q3',
        '{"facility":{"name":"facility","value":"Permian Basin Solar Farm","source":"m-rets","verified":true},"source":{"name":"source","value":"Solar Photovoltaic","source":"m-rets","verified":true},"generation":{"name":"generation","value":{"amount":42500,"unit":"MWh"},"source":"m-rets","verified":true},"period":{"name":"period","value":"2024-Q3","source":"m-rets","verified":true},"location":{"name":"location","value":"ERCOT","source":"m-rets","verified":true},"tracking":{"name":"tracking","value":"M-RETS","source":"m-rets","verified":true}}',
        'ACTIVE', 'sha256:f6a1b2c3d4e5', 'SHA-256', 'alice',
        '01912c6a-0005-7000-8000-000000000005', now() - interval '15 days', now() - interval '1 day');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0009-7000-8000-000000000009', 'energy', 'REC-WIND-IA-2024-Q4',
        'urn:far:energy:REC-WIND-IA-2024-Q4',
        '{"facility":{"name":"facility","value":"Story County Wind Farm","source":"wregis","verified":false},"source":{"name":"source","value":"Onshore Wind","source":"wregis","verified":false},"generation":{"name":"generation","value":{"amount":18200,"unit":"MWh"},"source":"wregis","verified":false},"period":{"name":"period","value":"2024-Q4","source":"wregis","verified":false}}',
        'DRAFT', 'sha256:c1e2a3f4b5d6', 'SHA-256', 'bob',
        '01912c6a-0005-7000-8000-000000000005', now() - interval '3 days', now() - interval '1 day');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0010-7000-8000-000000000010', 'energy', 'MIQ-PB-2024-0847',
        'urn:far:energy:MIQ-PB-2024-0847',
        '{"facility":{"name":"facility","value":"Permian Basin Unit 47","source":"miq","verified":true},"operator":{"name":"operator","value":"Pioneer Natural Resources","source":"miq","verified":true},"basin":{"name":"basin","value":"Permian Basin, West Texas","source":"miq","verified":true},"grade":{"name":"grade","value":"B","source":"miq","verified":true},"intensity":{"name":"intensity","value":{"amount":0.08,"unit":"%"},"source":"miq","verified":true},"auditor":{"name":"auditor","value":"ERM Certification and Verification Services","source":"miq","verified":true},"issued":{"name":"issued","value":"2024-06-15T00:00:00Z","source":"miq","verified":true},"expiry":{"name":"expiry","value":"2025-06-14T23:59:59Z","source":"miq","verified":true},"volume":{"name":"volume","value":{"amount":2400000,"unit":"MMBtu"},"source":"miq","verified":true},"standard":{"name":"standard","value":"MiQ Standard v2.0","source":"miq","verified":true}}',
        'ACTIVE', 'sha256:d2e3f4a5b6c7', 'SHA-256', 'alice',
        '01912c6a-0007-7000-8000-000000000007', now() - interval '25 days', now() - interval '4 days');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0011-7000-8000-000000000011', 'energy', 'MIQ-APP-2024-1203',
        'urn:far:energy:MIQ-APP-2024-1203',
        '{"facility":{"name":"facility","value":"Appalachia Central Processing Plant","source":"miq","verified":true},"operator":{"name":"operator","value":"EQT Corporation","source":"miq","verified":true},"basin":{"name":"basin","value":"Appalachian Basin, West Virginia","source":"miq","verified":true},"grade":{"name":"grade","value":"A","source":"miq","verified":true},"intensity":{"name":"intensity","value":{"amount":0.02,"unit":"%"},"source":"miq","verified":true},"auditor":{"name":"auditor","value":"SLR International Corporation","source":"miq","verified":true},"issued":{"name":"issued","value":"2024-09-01T00:00:00Z","source":"miq","verified":true},"expiry":{"name":"expiry","value":"2025-08-31T23:59:59Z","source":"miq","verified":true},"volume":{"name":"volume","value":{"amount":5800000,"unit":"MMBtu"},"source":"miq","verified":true},"standard":{"name":"standard","value":"MiQ Standard v2.0","source":"miq","verified":true}}',
        'ACTIVE', 'sha256:e3f4a5b6c7d8', 'SHA-256', 'bob',
        '01912c6a-0007-7000-8000-000000000007', now() - interval '12 days', now() - interval '2 days');

-- Ledger entries for Netherlands
INSERT INTO ledger_entries (id, urn, ledger, hash, published)
VALUES ('01912c6c-0001-7000-8000-000000000001', 'urn:far:carbon:VCS-2847-2024', 'database',
        'sha-256=:dG9jMmVlYTQ3YzhhMWIzZjJkNWE4N2YxYjRjOGVhMDk=:', now() - interval '28 days'),
       ('01912c6c-0002-7000-8000-000000000002', 'urn:far:carbon:GS-1192-2024', 'database',
        'sha-256=:ZTdmM2ExYjRjNWQ2ZThiOWYwYTJjN2Q0ZTVmNmExYjI=:', now() - interval '18 days'),
       ('01912c6c-0006-7000-8000-000000000006', 'urn:far:energy:REC-SOLAR-TX-2024-Q3', 'database',
        'sha-256=:ZjZhMWIyYzNkNGU1ZjZhMWIyYzNkNGU1ZjZhMWIyYzM=:', now() - interval '13 days'),
       ('01912c6c-0010-7000-8000-000000000010', 'urn:far:energy:MIQ-PB-2024-0847', 'database',
        'sha-256=:ZDJlM2Y0YTViNmM3ZDJlM2Y0YTViNmM3ZDJlM2Y0YTU=:', now() - interval '23 days'),
       ('01912c6c-0011-7000-8000-000000000011', 'urn:far:energy:MIQ-APP-2024-1203', 'database',
        'sha-256=:ZTNmNGE1YjZjN2Q4ZTNmNGE1YjZjN2Q4ZTNmNGE1YjY=:', now() - interval '10 days'),
       ('01912c6c-0012-7000-8000-000000000012', 'urn:far:carbon:VCS-3391-2024', 'database',
        'sha-256=:ZjRhNWI2YzdkOGU5ZjRhNWI2YzdkOGU5ZjRhNWI2Yzc=:', now() - interval '6 days'),
       ('01912c6c-0008-7000-8000-000000000008', 'urn:far:carbon:VCS-1455-2023', 'database',
        'sha-256=:YjFkMmYzYTRjNWU2YjFkMmYzYTRjNWU2YjFkMmYzYTQ=:', now() - interval '175 days'),
       ('01912c6c-0009-7000-8000-000000000009', 'urn:far:carbon:VCS-1455-2023', 'database',
        'sha-256=:ZjNhNGM1ZTZiMWQyZjNhNGM1ZTZiMWQyZjNhNGM1ZTY=:', now() - interval '30 days');
