-- V4__create_newsletters_table.sql
-- Newsletter profiles owned by senders, with subscription linkage

CREATE TABLE newsletters (
    id              UUID         PRIMARY KEY,
    sender_id       BIGINT       NOT NULL
                    REFERENCES users(id),
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    description     TEXT         NOT NULL DEFAULT '',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_newsletters_slug ON newsletters(slug);
CREATE INDEX idx_newsletters_sender_id ON newsletters(sender_id);

ALTER TABLE subscriptions ADD COLUMN newsletter_id UUID;

CREATE INDEX idx_subscriptions_newsletter_id ON subscriptions(newsletter_id);

COMMENT ON TABLE newsletters IS 'Public newsletter profiles owned by senders';
COMMENT ON COLUMN newsletters.slug IS 'URL-friendly unique identifier (e.g. tech-weekly)';
COMMENT ON COLUMN newsletters.sender_id IS 'Owner user (references users)';
COMMENT ON COLUMN subscriptions.newsletter_id IS 'Nullable FK linking legacy subscriptions to a newsletter';
