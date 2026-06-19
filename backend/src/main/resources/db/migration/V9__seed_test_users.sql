-- V9__seed_test_users.sql
-- Seed additional test users for development and testing
-- All passwords: Test@123 (BCrypt hash)
--
-- Users created:
--   alice@notify.com  — USER role, subscriber (no newsletter)
--   bob@notify.com    — USER role, sender (Tech Weekly newsletter)
--   carol@notify.com  — USER role, sender (Fashion Monthly newsletter)
--
-- Subscriptions created:
--   alice → Admin Announcements (CONFIRMED)
--   alice → Test Newsletter (CONFIRMED)
--   bob   → Admin Announcements (CONFIRMED)
--   carol → Test Newsletter (CONFIRMED)
--   PENDING subscription → Tech Weekly (from external email)

-- ============================================================
-- ALICE — subscriber only, no newsletter
-- ============================================================
INSERT INTO users (email, name, password, enabled)
VALUES (
    'alice@notify.com',
    'Alice Oliveira',
    '$2a$10$VU72w5/LNR4RCrBnzZspLO.TJ7v2oaB7zushMDWPoM6fn/bpkbFSa',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'alice@notify.com' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

-- ============================================================
-- BOB — sender (Tech Weekly)
-- ============================================================
INSERT INTO users (email, name, password, enabled)
VALUES (
    'bob@notify.com',
    'Bob Costa',
    '$2a$10$VU72w5/LNR4RCrBnzZspLO.TJ7v2oaB7zushMDWPoM6fn/bpkbFSa',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'bob@notify.com' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO newsletters (id, sender_id, name, slug, description)
SELECT
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    u.id,
    'Tech Weekly',
    'tech-weekly',
    'Weekly roundup of the latest in technology, programming, and software development'
FROM users u
WHERE u.email = 'bob@notify.com'
ON CONFLICT (slug) DO NOTHING;

-- ============================================================
-- CAROL — sender (Fashion Monthly)
-- ============================================================
INSERT INTO users (email, name, password, enabled)
VALUES (
    'carol@notify.com',
    'Carol Dias',
    '$2a$10$VU72w5/LNR4RCrBnzZspLO.TJ7v2oaB7zushMDWPoM6fn/bpkbFSa',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'carol@notify.com' AND r.name = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO newsletters (id, sender_id, name, slug, description)
SELECT
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    u.id,
    'Fashion Monthly',
    'fashion-monthly',
    'Curated fashion trends, style tips, and seasonal collections every month'
FROM users u
WHERE u.email = 'carol@notify.com'
ON CONFLICT (slug) DO NOTHING;

-- ============================================================
-- SUBSCRIPTIONS
-- ============================================================

-- Alice → Test Newsletter (CONFIRMED)
INSERT INTO subscriptions (id, newsletter_id, email, subscriber_id, status, token, expires_at, confirmed_at, created_at, updated_at)
SELECT
    gen_random_uuid(),
    n.id,
    'alice@notify.com',
    u.id,
    'CONFIRMED',
    gen_random_uuid(),
    NOW() + INTERVAL '24 hours',
    NOW(),
    NOW(),
    NOW()
FROM newsletters n, users u
WHERE n.slug = 'test-newsletter' AND u.email = 'alice@notify.com'
ON CONFLICT DO NOTHING;

-- Bob → Fashion Monthly (CONFIRMED)
INSERT INTO subscriptions (id, newsletter_id, email, subscriber_id, status, token, expires_at, confirmed_at, created_at, updated_at)
SELECT
    gen_random_uuid(),
    n.id,
    'bob@notify.com',
    u.id,
    'CONFIRMED',
    gen_random_uuid(),
    NOW() + INTERVAL '24 hours',
    NOW(),
    NOW(),
    NOW()
FROM newsletters n, users u
WHERE n.slug = 'fashion-monthly' AND u.email = 'bob@notify.com'
ON CONFLICT DO NOTHING;

-- Carol → Tech Weekly (CONFIRMED)
INSERT INTO subscriptions (id, newsletter_id, email, subscriber_id, status, token, expires_at, confirmed_at, created_at, updated_at)
SELECT
    gen_random_uuid(),
    n.id,
    'carol@notify.com',
    u.id,
    'CONFIRMED',
    gen_random_uuid(),
    NOW() + INTERVAL '24 hours',
    NOW(),
    NOW(),
    NOW()
FROM newsletters n, users u
WHERE n.slug = 'tech-weekly' AND u.email = 'carol@notify.com'
ON CONFLICT DO NOTHING;

-- Pending subscription → Tech Weekly (anonymous, external email)
INSERT INTO subscriptions (id, newsletter_id, email, subscriber_id, status, token, expires_at, created_at, updated_at)
SELECT
    gen_random_uuid(),
    n.id,
    'external@example.com',
    NULL,
    'PENDING',
    gen_random_uuid(),
    NOW() + INTERVAL '24 hours',
    NOW(),
    NOW()
FROM newsletters n
WHERE n.slug = 'tech-weekly'
ON CONFLICT DO NOTHING;
