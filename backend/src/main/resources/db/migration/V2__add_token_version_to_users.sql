-- V2__add_token_version_to_users.sql
-- Adds token_version for refresh token rotation
-- Each refresh increments the version; old tokens with stale version are rejected.

ALTER TABLE users
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN users.token_version IS 'Incremented on each refresh token rotation and password change. Used to invalidate stale refresh tokens.';
