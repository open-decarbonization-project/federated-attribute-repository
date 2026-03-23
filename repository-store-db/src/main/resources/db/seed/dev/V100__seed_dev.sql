-- Dev seed data: all certificates and ledger entries (single-instance dev mode)

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
VALUES ('01912c6b-0003-7000-8000-000000000003', 'iso', 'EMS-2024-ACME-001',
        'urn:far:iso:EMS-2024-ACME-001',
        '{"organization":{"name":"organization","value":"Acme Manufacturing Ltd","source":"bsi","verified":true},"scope":{"name":"scope","value":"Design, manufacture, and distribution of industrial components","source":"bsi","verified":true},"body":{"name":"body","value":"BSI Group","source":"bsi","verified":true},"issued":{"name":"issued","value":"2024-03-15T00:00:00Z","source":"bsi","verified":true},"expiry":{"name":"expiry","value":"2027-03-14T23:59:59Z","source":"bsi","verified":true},"accreditation":{"name":"accreditation","value":"UKAS 003","source":"bsi","verified":true}}',
        'ACTIVE', 'sha256:c3d4e5f6a1b2', 'SHA-256', 'alice',
        '01912c6a-0002-7000-8000-000000000002', now() - interval '60 days', now() - interval '10 days');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0004-7000-8000-000000000004', 'trade', 'FT-COF-ETH-2024-038',
        'urn:far:trade:FT-COF-ETH-2024-038',
        '{"producer":{"name":"producer","value":"Sidamo Coffee Cooperative Union","source":"fairtrade-intl","verified":true},"product":{"name":"product","value":"Arabica Coffee (green bean)","source":"fairtrade-intl","verified":true},"origin":{"name":"origin","value":"Ethiopia","source":"fairtrade-intl","verified":true},"premium":{"name":"premium","value":{"amount":0.20,"unit":"USD/kg"},"source":"fairtrade-intl","verified":true},"workers":{"name":"workers","value":3400,"source":"fairtrade-intl","verified":true},"organic":{"name":"organic","value":true,"source":"fairtrade-intl","verified":true}}',
        'ACTIVE', 'sha256:d4e5f6a1b2c3', 'SHA-256', 'bob',
        '01912c6a-0003-7000-8000-000000000003', now() - interval '45 days', now() - interval '3 days');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0005-7000-8000-000000000005', 'agriculture', 'ORG-CA-2024-1187',
        'urn:far:agriculture:ORG-CA-2024-1187',
        '{"operation":{"name":"operation","value":"Greenfield Valley Farms","source":"ccof","verified":true},"category":{"name":"category","value":"Crop","source":"ccof","verified":true},"area":{"name":"area","value":{"amount":240,"unit":"hectares"},"source":"ccof","verified":true},"certifier":{"name":"certifier","value":"CCOF Certification Services","source":"ccof","verified":true},"issued":{"name":"issued","value":"2024-01-10T00:00:00Z","source":"ccof","verified":true},"transitional":{"name":"transitional","value":false,"source":"ccof","verified":true}}',
        'ACTIVE', 'sha256:e5f6a1b2c3d4', 'SHA-256', 'alice',
        '01912c6a-0004-7000-8000-000000000004', now() - interval '90 days', now() - interval '15 days');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0006-7000-8000-000000000006', 'energy', 'REC-SOLAR-TX-2024-Q3',
        'urn:far:energy:REC-SOLAR-TX-2024-Q3',
        '{"facility":{"name":"facility","value":"Permian Basin Solar Farm","source":"m-rets","verified":true},"source":{"name":"source","value":"Solar Photovoltaic","source":"m-rets","verified":true},"generation":{"name":"generation","value":{"amount":42500,"unit":"MWh"},"source":"m-rets","verified":true},"period":{"name":"period","value":"2024-Q3","source":"m-rets","verified":true},"location":{"name":"location","value":"ERCOT","source":"m-rets","verified":true},"tracking":{"name":"tracking","value":"M-RETS","source":"m-rets","verified":true}}',
        'ACTIVE', 'sha256:f6a1b2c3d4e5', 'SHA-256', 'alice',
        '01912c6a-0005-7000-8000-000000000005', now() - interval '15 days', now() - interval '1 day');

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0007-7000-8000-000000000007', 'supply-chain', 'PROV-LI-BAT-2024-0892',
        'urn:far:supply-chain:PROV-LI-BAT-2024-0892',
        '{"product":{"name":"product","value":"Lithium-Ion Battery Pack (75 kWh)","source":"manufacturer","verified":true},"manufacturer":{"name":"manufacturer","value":"Northvolt AB","source":"manufacturer","verified":true},"origin":{"name":"origin","value":"Sweden","source":"manufacturer","verified":true},"batch":{"name":"batch","value":"NV-2024-Q3-0892","source":"manufacturer","verified":true},"quantity":{"name":"quantity","value":500,"source":"manufacturer","verified":true},"manufactured":{"name":"manufactured","value":"2024-08-22T00:00:00Z","source":"manufacturer","verified":true}}',
        'ACTIVE', 'sha256:a1c2e3b4d5f6', 'SHA-256', 'bob',
        '01912c6a-0006-7000-8000-000000000006', now() - interval '10 days', now() - interval '1 day');

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

INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0009-7000-8000-000000000009', 'energy', 'REC-WIND-IA-2024-Q4',
        'urn:far:energy:REC-WIND-IA-2024-Q4',
        '{"facility":{"name":"facility","value":"Story County Wind Farm","source":"wregis","verified":false},"source":{"name":"source","value":"Onshore Wind","source":"wregis","verified":false},"generation":{"name":"generation","value":{"amount":18200,"unit":"MWh"},"source":"wregis","verified":false},"period":{"name":"period","value":"2024-Q4","source":"wregis","verified":false}}',
        'DRAFT', 'sha256:c1e2a3f4b5d6', 'SHA-256', 'bob',
        '01912c6a-0005-7000-8000-000000000005', now() - interval '3 days', now() - interval '1 day');

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

-- Spain: Andalusia solar GO
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0030-7000-8000-000000000030', 'renewable-energy', 'GO-SOLAR-ES-2024-Q2',
        'urn:far:renewable-energy:GO-SOLAR-ES-2024-Q2',
        '{"facility":{"name":"facility","value":"Planta Solar La Serena","source":"cnmc","verified":true},"source":{"name":"source","value":"Solar Photovoltaic","source":"cnmc","verified":true},"generation":{"name":"generation","value":{"amount":28500,"unit":"MWh"},"source":"cnmc","verified":true},"period":{"name":"period","value":"2024-Q2","source":"cnmc","verified":true},"location":{"name":"location","value":"Andalusia, Spain","source":"cnmc","verified":true},"issuer":{"name":"issuer","value":"CNMC","source":"cnmc","verified":true},"tracking":{"name":"tracking","value":"AIB EECS","source":"cnmc","verified":true},"commissioned":{"name":"commissioned","value":"2021-06-15T00:00:00Z","source":"cnmc","verified":true}}',
        'ACTIVE', 'sha256:e6f7a8b9c0d1', 'SHA-256', 'alice',
        '01912c6a-0009-7000-8000-000000000009', now() - interval '40 days', now() - interval '5 days');

-- Spain: Galicia wind GO
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0031-7000-8000-000000000031', 'renewable-energy', 'GO-WIND-ES-2024-Q3',
        'urn:far:renewable-energy:GO-WIND-ES-2024-Q3',
        '{"facility":{"name":"facility","value":"Parque Eólico Serra do Xistral","source":"cnmc","verified":true},"source":{"name":"source","value":"Onshore Wind","source":"cnmc","verified":true},"generation":{"name":"generation","value":{"amount":35200,"unit":"MWh"},"source":"cnmc","verified":true},"period":{"name":"period","value":"2024-Q3","source":"cnmc","verified":true},"location":{"name":"location","value":"Galicia, Spain","source":"cnmc","verified":true},"issuer":{"name":"issuer","value":"CNMC","source":"cnmc","verified":true},"tracking":{"name":"tracking","value":"AIB EECS","source":"cnmc","verified":true},"commissioned":{"name":"commissioned","value":"2019-11-20T00:00:00Z","source":"cnmc","verified":true}}',
        'ACTIVE', 'sha256:f7a8b9c0d1e2', 'SHA-256', 'carol',
        '01912c6a-0009-7000-8000-000000000009', now() - interval '25 days', now() - interval '3 days');

-- Spain: Castilla-La Mancha solar GO
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0032-7000-8000-000000000032', 'renewable-energy', 'GO-SOLAR-ES-2024-Q4',
        'urn:far:renewable-energy:GO-SOLAR-ES-2024-Q4',
        '{"facility":{"name":"facility","value":"Planta Fotovoltaica Núñez de Balboa","source":"cnmc","verified":true},"source":{"name":"source","value":"Solar Photovoltaic","source":"cnmc","verified":true},"generation":{"name":"generation","value":{"amount":52000,"unit":"MWh"},"source":"cnmc","verified":true},"period":{"name":"period","value":"2024-Q4","source":"cnmc","verified":true},"location":{"name":"location","value":"Castilla-La Mancha, Spain","source":"cnmc","verified":true},"issuer":{"name":"issuer","value":"CNMC","source":"cnmc","verified":true},"tracking":{"name":"tracking","value":"AIB EECS","source":"cnmc","verified":true},"commissioned":{"name":"commissioned","value":"2020-03-01T00:00:00Z","source":"cnmc","verified":true}}',
        'ACTIVE', 'sha256:a8b9c0d1e2f3', 'SHA-256', 'bob',
        '01912c6a-0009-7000-8000-000000000009', now() - interval '10 days', now() - interval '1 day');

-- CXL: Dutch biomethane
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0040-7000-8000-000000000040', 'ngg', 'NGG-BIO-NL-2024-001',
        'urn:far:ngg:NGG-BIO-NL-2024-001',
        '{"facility":{"name":"facility","value":"Groningen Biogas Plant","source":"vertogas","verified":true},"operator":{"name":"operator","value":"Engie Energie Nederland","source":"vertogas","verified":true},"feedstock":{"name":"feedstock","value":"Agricultural waste and manure","source":"vertogas","verified":true},"grade":{"name":"grade","value":"A+","source":"vertogas","verified":true},"intensity":{"name":"intensity","value":{"amount":12.5,"unit":"gCO2/kWh"},"source":"vertogas","verified":true},"volume":{"name":"volume","value":{"amount":8500,"unit":"MWh"},"source":"vertogas","verified":true},"auditor":{"name":"auditor","value":"KIWA Technology","source":"vertogas","verified":true},"issued":{"name":"issued","value":"2024-05-01T00:00:00Z","source":"vertogas","verified":true},"expiry":{"name":"expiry","value":"2025-04-30T23:59:59Z","source":"vertogas","verified":true}}',
        'ACTIVE', 'sha256:b9c0d1e2f3a4', 'SHA-256', 'alice',
        '01912c6a-0008-7000-8000-000000000008', now() - interval '50 days', now() - interval '7 days');

-- CXL: German green hydrogen
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0041-7000-8000-000000000041', 'ngg', 'NGG-H2-DE-2024-042',
        'urn:far:ngg:NGG-H2-DE-2024-042',
        '{"facility":{"name":"facility","value":"Wesseling Green Hydrogen Hub","source":"dena","verified":true},"operator":{"name":"operator","value":"Shell Deutschland GmbH","source":"dena","verified":true},"feedstock":{"name":"feedstock","value":"Electrolysis (wind-powered)","source":"dena","verified":true},"grade":{"name":"grade","value":"A","source":"dena","verified":true},"intensity":{"name":"intensity","value":{"amount":3.8,"unit":"gCO2/kWh"},"source":"dena","verified":true},"volume":{"name":"volume","value":{"amount":4200,"unit":"MWh"},"source":"dena","verified":true},"auditor":{"name":"auditor","value":"TÜV SÜD","source":"dena","verified":true},"issued":{"name":"issued","value":"2024-07-15T00:00:00Z","source":"dena","verified":true},"expiry":{"name":"expiry","value":"2025-07-14T23:59:59Z","source":"dena","verified":true}}',
        'ACTIVE', 'sha256:c0d1e2f3a4b5', 'SHA-256', 'carol',
        '01912c6a-0008-7000-8000-000000000008', now() - interval '35 days', now() - interval '4 days');

-- CXL: French synthetic natural gas
INSERT INTO certificates (id, namespace, identifier, urn, attributes, status, integrity_digest, integrity_algorithm,
                          owner, schema_id, created, modified)
VALUES ('01912c6b-0042-7000-8000-000000000042', 'ngg', 'NGG-SNG-FR-2024-018',
        'urn:far:ngg:NGG-SNG-FR-2024-018',
        '{"facility":{"name":"facility","value":"Port-Jérôme Methanation Plant","source":"grdf","verified":true},"operator":{"name":"operator","value":"Storengy SA","source":"grdf","verified":true},"feedstock":{"name":"feedstock","value":"CO2 capture + green hydrogen","source":"grdf","verified":true},"grade":{"name":"grade","value":"B+","source":"grdf","verified":true},"intensity":{"name":"intensity","value":{"amount":28.3,"unit":"gCO2/kWh"},"source":"grdf","verified":true},"volume":{"name":"volume","value":{"amount":3100,"unit":"MWh"},"source":"grdf","verified":true},"auditor":{"name":"auditor","value":"Bureau Veritas","source":"grdf","verified":true},"issued":{"name":"issued","value":"2024-09-10T00:00:00Z","source":"grdf","verified":true},"expiry":{"name":"expiry","value":"2025-09-09T23:59:59Z","source":"grdf","verified":true}}',
        'ACTIVE', 'sha256:d1e2f3a4b5c6', 'SHA-256', 'dave',
        '01912c6a-0008-7000-8000-000000000008', now() - interval '20 days', now() - interval '2 days');

-- All ledger entries
INSERT INTO ledger_entries (id, urn, ledger, hash, published)
VALUES ('01912c6c-0001-7000-8000-000000000001', 'urn:far:carbon:VCS-2847-2024', 'database',
        'sha-256=:dG9jMmVlYTQ3YzhhMWIzZjJkNWE4N2YxYjRjOGVhMDk=:', now() - interval '28 days'),
       ('01912c6c-0002-7000-8000-000000000002', 'urn:far:carbon:GS-1192-2024', 'database',
        'sha-256=:ZTdmM2ExYjRjNWQ2ZThiOWYwYTJjN2Q0ZTVmNmExYjI=:', now() - interval '18 days'),
       ('01912c6c-0003-7000-8000-000000000003', 'urn:far:iso:EMS-2024-ACME-001', 'database',
        'sha-256=:YjRjNWQ2ZThiOWYwYTJjN2Q0ZTVmNmExYjJjM2Q0ZTU=:', now() - interval '55 days'),
       ('01912c6c-0004-7000-8000-000000000004', 'urn:far:trade:FT-COF-ETH-2024-038', 'database',
        'sha-256=:ZDRlNWY2YTFiMmMzZDRlNWY2YTFiMmMzZDRlNWY2YTE=:', now() - interval '42 days'),
       ('01912c6c-0005-7000-8000-000000000005', 'urn:far:agriculture:ORG-CA-2024-1187', 'database',
        'sha-256=:ZTVmNmExYjJjM2Q0ZTVmNmExYjJjM2Q0ZTVmNmExYjI=:', now() - interval '85 days'),
       ('01912c6c-0006-7000-8000-000000000006', 'urn:far:energy:REC-SOLAR-TX-2024-Q3', 'database',
        'sha-256=:ZjZhMWIyYzNkNGU1ZjZhMWIyYzNkNGU1ZjZhMWIyYzM=:', now() - interval '13 days'),
       ('01912c6c-0007-7000-8000-000000000007', 'urn:far:supply-chain:PROV-LI-BAT-2024-0892', 'database',
        'sha-256=:YTFjMmUzYjRkNWY2YTFjMmUzYjRkNWY2YTFjMmUzYjQ=:', now() - interval '8 days'),
       ('01912c6c-0010-7000-8000-000000000010', 'urn:far:energy:MIQ-PB-2024-0847', 'database',
        'sha-256=:ZDJlM2Y0YTViNmM3ZDJlM2Y0YTViNmM3ZDJlM2Y0YTU=:', now() - interval '23 days'),
       ('01912c6c-0011-7000-8000-000000000011', 'urn:far:energy:MIQ-APP-2024-1203', 'database',
        'sha-256=:ZTNmNGE1YjZjN2Q4ZTNmNGE1YjZjN2Q4ZTNmNGE1YjY=:', now() - interval '10 days'),
       ('01912c6c-0012-7000-8000-000000000012', 'urn:far:carbon:VCS-3391-2024', 'database',
        'sha-256=:ZjRhNWI2YzdkOGU5ZjRhNWI2YzdkOGU5ZjRhNWI2Yzc=:', now() - interval '6 days'),
       ('01912c6c-0008-7000-8000-000000000008', 'urn:far:carbon:VCS-1455-2023', 'database',
        'sha-256=:YjFkMmYzYTRjNWU2YjFkMmYzYTRjNWU2YjFkMmYzYTQ=:', now() - interval '175 days'),
       ('01912c6c-0009-7000-8000-000000000009', 'urn:far:carbon:VCS-1455-2023', 'database',
        'sha-256=:ZjNhNGM1ZTZiMWQyZjNhNGM1ZTZiMWQyZjNhNGM1ZTY=:', now() - interval '30 days'),
       ('01912c6c-0020-7000-8000-000000000020', 'urn:far:agriculture:BIO-FR-2024-0456', 'database',
        'sha-256=:YTJiM2M0ZDVlNmY3YTJiM2M0ZDVlNmY3YTJiM2M0ZDU=:', now() - interval '55 days'),
       ('01912c6c-0021-7000-8000-000000000021', 'urn:far:trade:FT-CHO-GH-2024-102', 'database',
        'sha-256=:YjNjNGQ1ZTZmN2E4YjNjNGQ1ZTZmN2E4YjNjNGQ1ZTY=:', now() - interval '25 days'),
       ('01912c6c-0022-7000-8000-000000000022', 'urn:far:iso:QMS-2024-VOLVO-003', 'database',
        'sha-256=:YzRkNWU2ZjdhOGI5YzRkNWU2ZjdhOGI5YzRkNWU2Zjc=:', now() - interval '40 days'),
       ('01912c6c-0023-7000-8000-000000000023', 'urn:far:supply-chain:PROV-EV-MOT-2024-1547', 'database',
        'sha-256=:ZDVlNmY3YThiOWMwZDVlNmY3YThiOWMwZDVlNmY3YTg=:', now() - interval '18 days'),
       ('01912c6c-0030-7000-8000-000000000030', 'urn:far:renewable-energy:GO-SOLAR-ES-2024-Q2', 'database',
        'sha-256=:ZTZmN2E4YjljMGQxZTZmN2E4YjljMGQxZTZmN2E4Yjk=:', now() - interval '35 days'),
       ('01912c6c-0031-7000-8000-000000000031', 'urn:far:renewable-energy:GO-WIND-ES-2024-Q3', 'database',
        'sha-256=:ZjdhOGI5YzBkMWUyZjdhOGI5YzBkMWUyZjdhOGI5YzA=:', now() - interval '22 days'),
       ('01912c6c-0032-7000-8000-000000000032', 'urn:far:renewable-energy:GO-SOLAR-ES-2024-Q4', 'database',
        'sha-256=:YThiOWMwZDFlMmYzYThiOWMwZDFlMmYzYThiOWMwZDE=:', now() - interval '8 days'),
       ('01912c6c-0040-7000-8000-000000000040', 'urn:far:ngg:NGG-BIO-NL-2024-001', 'database',
        'sha-256=:YjljMGQxZTJmM2E0YjljMGQxZTJmM2E0YjljMGQxZTI=:', now() - interval '45 days'),
       ('01912c6c-0041-7000-8000-000000000041', 'urn:far:ngg:NGG-H2-DE-2024-042', 'database',
        'sha-256=:YzBkMWUyZjNhNGI1YzBkMWUyZjNhNGI1YzBkMWUyZjM=:', now() - interval '30 days'),
       ('01912c6c-0042-7000-8000-000000000042', 'urn:far:ngg:NGG-SNG-FR-2024-018', 'database',
        'sha-256=:ZDFlMmYzYTRiNWM2ZDFlMmYzYTRiNWM2ZDFlMmYzYTQ=:', now() - interval '17 days');
