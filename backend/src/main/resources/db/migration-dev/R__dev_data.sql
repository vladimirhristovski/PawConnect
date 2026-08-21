INSERT INTO users (username, email, password, first_name, last_name, is_active)
SELECT 'admin',
       'admin@pawconnect.com',
       '$2a$10$7Z9p5JNlXwOqK7VZ4r5nA.jxXUvW3YmLnWqEfJtR8sNQo2P6J0K9e',
       'Admin',
       'User',
       true WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u,
     roles r
WHERE u.username = 'admin'
  AND r.name = 'ADMIN' ON CONFLICT (user_id, role_id) DO NOTHING;