-- V7__create_campaigns_table.sql
-- Campaign management for newsletter sending
-- Status flow: DRAFT → PENDING → PUBLISHED → SENT
-- scheduled_at is optional, used for future scheduling (Flow 6)

CREATE TABLE campaigns (
    id              UUID         PRIMARY KEY,
    newsletter_id   UUID         NOT NULL REFERENCES newsletters(id) ON DELETE CASCADE,
    subject         VARCHAR(200) NOT NULL,
    content         TEXT         NOT NULL DEFAULT '',
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'SENT')),
    scheduled_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_campaigns_newsletter_id ON campaigns(newsletter_id);

COMMENT ON TABLE campaigns IS 'Email campaigns created by newsletter senders';
COMMENT ON COLUMN campaigns.status IS 'DRAFT = editing, PENDING = submitted, PUBLISHED = ready to send, SENT = delivered (via Flow 6)';
COMMENT ON COLUMN campaigns.scheduled_at IS 'Optional future date for scheduled sending (used in Flow 6)';
COMMENT ON COLUMN campaigns.newsletter_id IS 'Parent newsletter (cascade delete)';
