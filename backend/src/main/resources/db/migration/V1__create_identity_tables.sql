-- V1__create_identity_tables.sql
-- Identity & Access Management schema
-- Tables: users, roles, user_roles, role_permissions
-- Seed: ADMIN + USER roles, admin user

-- ============================================================
-- ROLES
-- ============================================================
CREATE TABLE roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL DEFAULT ''
);

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- USER_ROLES (many-to-many)
-- ============================================================
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- ROLE_PERMISSIONS (granular permissions per role)
-- ============================================================
CREATE TABLE role_permissions (
    role_id    BIGINT       NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission)
);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Roles
INSERT INTO roles (name, description)
VALUES ('ADMIN', 'Full system access'),
       ('USER',  'Standard user access')
ON CONFLICT (name) DO NOTHING;

-- Permissions for ADMIN role (id = 1)
INSERT INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN (VALUES
    ('user:read'),
    ('user:create'),
    ('user:update'),
    ('user:delete'),
    ('role:manage')
) AS p(permission)
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission) DO NOTHING;

-- Permissions for USER role (id = 2)
INSERT INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN (VALUES
    ('user:read'),
    ('user:update')
) AS p(permission)
WHERE r.name = 'USER'
ON CONFLICT (role_id, permission) DO NOTHING;

-- Seed admin user
-- Password: Admin@123 (BCrypt hash)
INSERT INTO users (email, name, password, enabled)
VALUES (
    'admin@notify.com',
    'System Administrator',
    '$2a$10$DlYT41YAH7xDTBD7yBJTWucE6zJTNgVHc00SHGJ0Q4RPybAa4ApJe',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

-- Assign ADMIN role to seed admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@notify.com' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
