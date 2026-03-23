-- Seed data: common certification schemas

-- Carbon Credit Certification
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES ('01912c6a-0001-7000-8000-000000000001', 'carbon', 'carbon-credit', 'Voluntary carbon credit certification',
        1, '[
    {"name":"project","label":"Project Name","description":"Name of the carbon offset project","type":{"name":"string"},"required":true,"position":0},
    {"name":"volume","label":"Volume","description":"Quantity of carbon credits in metric tonnes CO2e","type":{"name":"quantity","unit":"tCO2e"},"required":true,"position":1},
    {"name":"vintage","label":"Vintage Year","description":"Year the emission reduction occurred","type":{"name":"string"},"required":true,"position":2,"policy":{"kind":"masked"}},
    {"name":"methodology","label":"Methodology","description":"Approved methodology used for quantification","type":{"name":"string"},"required":true,"position":3},
    {"name":"repository","label":"Repository","description":"Issuing repository (e.g. Verra, Gold Standard)","type":{"name":"string"},"required":false,"position":4},
    {"name":"serial","label":"Serial Number","description":"Unique serial number from the repository","type":{"name":"string"},"required":false,"position":5,"policy":{"kind":"credential","role":"repository-admin"}},
    {"name":"retired","label":"Retired","description":"Whether the credits have been retired","type":{"name":"boolean"},"required":false,"position":6}
  ]', true, 'alice', now(), now());

-- ISO 14001 Environmental Management
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES ('01912c6a-0002-7000-8000-000000000002', 'iso', 'iso-14001',
        'ISO 14001 Environmental Management System certification',
        1, '[
    {"name":"organization","label":"Organization","description":"Certified organization name","type":{"name":"string"},"required":true,"position":0},
    {"name":"scope","label":"Scope","description":"Scope of the certification","type":{"name":"string"},"required":true,"position":1},
    {"name":"body","label":"Certification Body","description":"Accredited certification body","type":{"name":"string"},"required":true,"position":2},
    {"name":"issued","label":"Issue Date","description":"Date certification was issued","type":{"name":"datetime"},"required":true,"position":3},
    {"name":"expiry","label":"Expiry Date","description":"Date certification expires","type":{"name":"datetime"},"required":true,"position":4},
    {"name":"accreditation","label":"Accreditation Number","description":"Certification body accreditation number","type":{"name":"string"},"required":false,"position":5}
  ]', true, 'alice', now(), now());

-- Fair Trade Certification
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES ('01912c6a-0003-7000-8000-000000000003', 'trade', 'fair-trade', 'Fair trade product certification',
        1, '[
    {"name":"producer","label":"Producer","description":"Name of the producer or cooperative","type":{"name":"string"},"required":true,"position":0},
    {"name":"product","label":"Product","description":"Certified product type","type":{"name":"string"},"required":true,"position":1},
    {"name":"origin","label":"Country of Origin","description":"Country where the product is produced","type":{"name":"string"},"required":true,"position":2},
    {"name":"premium","label":"Fair Trade Premium","description":"Premium paid per unit","type":{"name":"quantity","unit":"USD/kg"},"required":false,"position":3},
    {"name":"workers","label":"Worker Count","description":"Number of workers covered","type":{"name":"numeric"},"required":false,"position":4},
    {"name":"organic","label":"Organic","description":"Whether the product is also organically certified","type":{"name":"boolean"},"required":false,"position":5}
  ]', true, 'alice', now(), now());

-- Organic Certification
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES ('01912c6a-0004-7000-8000-000000000004', 'agriculture', 'organic',
        'Organic agriculture certification (USDA/EU equivalent)',
        1, '[
    {"name":"operation","label":"Operation Name","description":"Name of the certified farm or operation","type":{"name":"string"},"required":true,"position":0},
    {"name":"category","label":"Category","description":"Crop, livestock, handling, or wild crop","type":{"name":"string"},"required":true,"position":1},
    {"name":"area","label":"Certified Area","description":"Area under organic management","type":{"name":"quantity","unit":"hectares"},"required":true,"position":2},
    {"name":"certifier","label":"Certifying Agent","description":"USDA-accredited certifying agent","type":{"name":"string"},"required":true,"position":3},
    {"name":"issued","label":"Issue Date","description":"Date certification was granted","type":{"name":"datetime"},"required":true,"position":4},
    {"name":"transitional","label":"Transitional","description":"Whether the operation is in transition period","type":{"name":"boolean"},"required":false,"position":5}
  ]', true, 'alice', now(), now());

-- Renewable Energy Certificate
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES ('01912c6a-0005-7000-8000-000000000005', 'energy', 'renewable-energy', 'Renewable Energy Certificate (REC / GO)',
        1, '[
    {"name":"facility","label":"Facility","description":"Generating facility name","type":{"name":"string"},"required":true,"position":0},
    {"name":"source","label":"Energy Source","description":"Type of renewable energy (solar, wind, hydro, etc.)","type":{"name":"string"},"required":true,"position":1},
    {"name":"generation","label":"Generation","description":"Energy generated","type":{"name":"quantity","unit":"MWh"},"required":true,"position":2},
    {"name":"period","label":"Generation Period","description":"Month/year of generation","type":{"name":"string"},"required":true,"position":3},
    {"name":"location","label":"Grid Region","description":"Grid region or balancing area","type":{"name":"string"},"required":false,"position":4},
    {"name":"tracking","label":"Tracking System","description":"REC tracking system (e.g. M-RETS, WREGIS)","type":{"name":"string"},"required":false,"position":5},
    {"name":"commissioned","label":"Commissioned Date","description":"Date facility was commissioned","type":{"name":"datetime"},"required":false,"position":6}
  ]', true, 'alice', now(), now());

-- Supply Chain Provenance
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES ('01912c6a-0006-7000-8000-000000000006', 'supply-chain', 'provenance',
        'Supply chain provenance and traceability',
        1, '[
    {"name":"product","label":"Product","description":"Product name or SKU","type":{"name":"string"},"required":true,"position":0},
    {"name":"manufacturer","label":"Manufacturer","description":"Manufacturing entity","type":{"name":"string"},"required":true,"position":1},
    {"name":"origin","label":"Country of Origin","description":"Country of manufacture","type":{"name":"string"},"required":true,"position":2},
    {"name":"batch","label":"Batch Number","description":"Production batch identifier","type":{"name":"string"},"required":true,"position":3},
    {"name":"quantity","label":"Quantity","description":"Number of units in batch","type":{"name":"numeric"},"required":false,"position":4},
    {"name":"manufactured","label":"Manufacture Date","description":"Date of manufacture","type":{"name":"datetime"},"required":false,"position":5}
  ]', true, 'alice', now(), now());

-- MiQ Methane Certification
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES ('01912c6a-0007-7000-8000-000000000007', 'energy', 'miq', 'MiQ methane emissions certification for natural gas',
        1, '[
    {"name":"facility","label":"Facility","description":"Production facility or asset name","type":{"name":"string"},"required":true,"position":0},
    {"name":"operator","label":"Operator","description":"Facility operator","type":{"name":"string"},"required":true,"position":1},
    {"name":"basin","label":"Basin","description":"Producing basin or region","type":{"name":"string"},"required":true,"position":2},
    {"name":"grade","label":"MiQ Grade","description":"Methane emissions grade (A through F)","type":{"name":"string"},"required":true,"position":3},
    {"name":"intensity","label":"Methane Intensity","description":"Methane emissions intensity","type":{"name":"quantity","unit":"%"},"required":true,"position":4},
    {"name":"auditor","label":"Auditor","description":"Independent auditing body","type":{"name":"string"},"required":true,"position":5},
    {"name":"issued","label":"Issue Date","description":"Date the certificate was issued","type":{"name":"datetime"},"required":true,"position":6},
    {"name":"expiry","label":"Expiry Date","description":"Date the certificate expires","type":{"name":"datetime"},"required":true,"position":7},
    {"name":"volume","label":"Certified Volume","description":"Volume of natural gas covered","type":{"name":"quantity","unit":"MMBtu"},"required":false,"position":8},
    {"name":"standard","label":"Standard Version","description":"MiQ Standard version used for grading","type":{"name":"string"},"required":false,"position":9}
  ]', true, 'alice', now(), now());

-- Next Generation Gas (NGG) Certification
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES ('01912c6a-0008-7000-8000-000000000008', 'ngg', 'ngg', 'Next Generation Gas certification for low-carbon gas',
        1, '[
    {"name":"facility","label":"Facility","description":"Production or injection facility","type":{"name":"string"},"required":true,"position":0},
    {"name":"operator","label":"Operator","description":"Facility operator","type":{"name":"string"},"required":true,"position":1},
    {"name":"feedstock","label":"Feedstock","description":"Input feedstock type (biomethane, hydrogen, SNG)","type":{"name":"string"},"required":true,"position":2},
    {"name":"grade","label":"Grade","description":"Gas quality grade","type":{"name":"string"},"required":true,"position":3},
    {"name":"intensity","label":"Carbon Intensity","description":"Carbon intensity of production","type":{"name":"quantity","unit":"gCO2/kWh"},"required":true,"position":4},
    {"name":"volume","label":"Volume","description":"Certified gas volume","type":{"name":"quantity","unit":"MWh"},"required":true,"position":5},
    {"name":"auditor","label":"Auditor","description":"Independent auditing body","type":{"name":"string"},"required":true,"position":6},
    {"name":"issued","label":"Issue Date","description":"Date the certificate was issued","type":{"name":"datetime"},"required":true,"position":7},
    {"name":"expiry","label":"Expiry Date","description":"Date the certificate expires","type":{"name":"datetime"},"required":true,"position":8}
  ]', true, 'alice', now(), now());

-- Renewable Energy Guarantee of Origin
INSERT INTO schemas (id, namespace, name, description, version, fields, active, owner, created, modified)
VALUES ('01912c6a-0009-7000-8000-000000000009', 'renewable-energy', 'guarantee-of-origin',
        'EU Guarantee of Origin for renewable energy',
        1, '[
    {"name":"facility","label":"Facility","description":"Generating facility name","type":{"name":"string"},"required":true,"position":0},
    {"name":"source","label":"Energy Source","description":"Type of renewable energy (solar, wind, hydro, etc.)","type":{"name":"string"},"required":true,"position":1},
    {"name":"generation","label":"Generation","description":"Energy generated","type":{"name":"quantity","unit":"MWh"},"required":true,"position":2},
    {"name":"period","label":"Generation Period","description":"Month/quarter/year of generation","type":{"name":"string"},"required":true,"position":3},
    {"name":"location","label":"Location","description":"Geographic location of facility","type":{"name":"string"},"required":true,"position":4},
    {"name":"issuer","label":"Issuer","description":"National issuing body","type":{"name":"string"},"required":true,"position":5},
    {"name":"tracking","label":"Tracking System","description":"GO tracking system (e.g. AIB, CNMC)","type":{"name":"string"},"required":false,"position":6},
    {"name":"commissioned","label":"Commissioned Date","description":"Date facility was commissioned","type":{"name":"datetime"},"required":false,"position":7}
  ]', true, 'alice', now(), now());

-- Backfill schema versions from seed data
INSERT INTO schema_versions (schema_id, version, description, fields, active, created)
SELECT id, version, description, fields, active, modified
FROM schemas;

-- Backfill field policies from seed data
INSERT INTO field_policies (schema_id, field, kind, role)
SELECT s.id,
       f ->>'name', COALESCE (f->'policy'->>'kind', 'public'), f->'policy'->>'role'
FROM schemas s, jsonb_array_elements(s.fields) AS f
WHERE f->'policy' IS NOT NULL AND f->'policy'->>'kind' != 'public';
