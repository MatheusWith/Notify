-- V3__create_subscriptions_table.sql
-- Newsletter subscription with email confirmation flow

CREATE TABLE subscriptions (
    id              UUID        PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED')),
    token           UUID         NOT NULL,
    expires_at      TIMESTAMP    NOT NULL,
    confirmed_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_email ON subscriptions(email);
CREATE INDEX idx_subscriptions_token ON subscriptions(token);

COMMENT ON TABLE subscriptions IS 'Newsletter email subscriptions with confirmation flow';
COMMENT ON COLUMN subscriptions.status IS 'PENDING = awaiting confirmation, CONFIRMED = active, EXPIRED = token expired';
COMMENT ON COLUMN subscriptions.token IS 'UUID v4 confirmation token, regenerated on resend';
COMMENT ON COLUMN subscriptions.expires_at IS 'Token expiration timestamp (created_at + 24h)';
