-- Spain peers: connect to all other registries
INSERT INTO peers (identity, endpoint, namespaces, seen, created)
VALUES ('http://netherlands:8081', 'http://netherlands:8081', '["carbon","energy"]', now(), now()),
       ('http://france:8081', 'http://france:8081', '["agriculture","trade"]', now(), now()),
       ('http://belgium:8081', 'http://belgium:8081', '["iso","supply-chain"]', now(), now()),
       ('http://cxl:8081', 'http://cxl:8081', '["ngg"]', now(), now());
