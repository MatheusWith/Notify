-- V5__seed_admin_newsletter.sql
-- Seed a newsletter for the admin user (created in V1)
-- Uses a deterministic UUID v4 for the newsletter ID

INSERT INTO newsletters (id, sender_id, name, slug, description)
SELECT
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    u.id,
    'Admin Announcements',
    'admin-announcements',
    'Official announcements from the Notify team'
FROM users u
WHERE u.email = 'admin@notify.com'
ON CONFLICT (slug) DO NOTHING;
