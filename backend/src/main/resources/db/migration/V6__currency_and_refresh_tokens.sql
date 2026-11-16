ALTER TABLE groups ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'USD';
ALTER TABLE expenses ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'USD';
ALTER TABLE settlements ADD COLUMN reversed_at TIMESTAMPTZ;

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_expires_at_idx ON refresh_tokens (expires_at);
