-- V6__add_subscriber_id_to_subscriptions.sql
-- Adds subscriber_id for authenticated user subscriptions
-- Nullable: anonymous subscriptions keep only email; authenticated subscriptions link to users

ALTER TABLE subscriptions
    ADD COLUMN subscriber_id BIGINT;

CREATE INDEX idx_subscriptions_subscriber_id ON subscriptions(subscriber_id);

COMMENT ON COLUMN subscriptions.subscriber_id IS 'Optional FK linking subscription to an authenticated user (references users.id). Null for anonymous subscriptions.';
