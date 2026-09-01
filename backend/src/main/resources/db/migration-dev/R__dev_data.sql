INSERT INTO users (username, email, password, first_name, last_name, is_active)
SELECT 'admin',
       'admin@pawconnect.com',
       '$2a$10$.ZYeX2YXtmy9UbEH.FQdWuim8X0JM7nil56cmML/J3cKAX3bLc8sW',
       'Admin',
       'User',
       true WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u,
     roles r
WHERE u.username = 'admin'
  AND r.name = 'ADMIN' ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO countries (code, name)
VALUES ('MK', 'Macedonia'),
       ('UK', 'United Kingdom') ON CONFLICT (code) DO NOTHING;

DO
$$
DECLARE
mk_id BIGINT;
BEGIN
SELECT id
INTO mk_id
FROM countries
WHERE code = 'MK';

INSERT INTO cities (code, name, country_id)
VALUES ('SK', 'Skopje', mk_id),
       ('BT', 'Bitola', mk_id),
       ('OH', 'Ohrid', mk_id),
       ('TE', 'Tetovo', mk_id),
       ('KU', 'Kumanovo', mk_id),
       ('PR', 'Prilep', mk_id),
       ('VE', 'Veles', mk_id),
       ('ST', 'Štip', mk_id),
       ('STR', 'Strumica', mk_id),
       ('GV', 'Gevgelija', mk_id) ON CONFLICT (code) DO NOTHING;
END $$;

DO
$$
DECLARE
skopje_id BIGINT;
BEGIN
SELECT id
INTO skopje_id
FROM cities
WHERE code = 'SK';

INSERT INTO municipalities (code, name, city_id)
VALUES ('SK-CENTAR', 'Centar', skopje_id),
       ('SK-KARPOSH', 'Karpoš', skopje_id),
       ('SK-AERODROM', 'Aerodrom', skopje_id),
       ('SK-GAZI_BABA', 'Gazi Baba', skopje_id),
       ('SK-KISELA_VODA', 'Kisela Voda', skopje_id),
       ('SK-CAIR', 'Čair', skopje_id),
       ('SK-BUTEL', 'Butel', skopje_id),
       ('SK-SUTO_ORIZARI', 'Šuto Orizari', skopje_id) ON CONFLICT (code) DO NOTHING;
END $$;