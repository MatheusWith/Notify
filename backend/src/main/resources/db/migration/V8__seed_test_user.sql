-- V8__seed_test_user.sql
-- Seed a test user with USER role and a sample newsletter
-- Password: Test@123 (BCrypt hash)

-- Insert test user
INSERT INTO users (email, name, password, enabled)
VALUES (
    'test@notify.com',
    'Test User',
    '$2a$10$VU72w5/LNR4RCrBnzZspLO.TJ7v2oaB7zushMDWPoM6fn/bpkbFSa',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

-- Assign USER role to test user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'test@notify.com' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- Seed a test newsletter for the test user
INSERT INTO newsletters (id, sender_id, name, slug, description)
SELECT
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    u.id,
    'Test Newsletter',
    'test-newsletter',
    'A sample newsletter for testing purposes'
FROM users u
WHERE u.email = 'test@notify.com'
ON CONFLICT (slug) DO NOTHING;
