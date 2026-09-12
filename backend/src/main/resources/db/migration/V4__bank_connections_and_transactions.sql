CREATE TABLE bank_connections (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plaid_item_id VARCHAR(255) NOT NULL UNIQUE,
    institution_id VARCHAR(255),
    institution_name VARCHAR(255),
    encrypted_access_token TEXT NOT NULL,
    sync_cursor TEXT,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(100),
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT bank_connections_status_check CHECK (status IN ('HEALTHY', 'ERROR'))
);

CREATE INDEX bank_connections_user_id_idx ON bank_connections (user_id);

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL REFERENCES bank_connections (id) ON DELETE CASCADE,
    plaid_account_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    official_name VARCHAR(255),
    mask VARCHAR(20),
    account_type VARCHAR(50) NOT NULL,
    subtype VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX bank_accounts_connection_id_idx ON bank_accounts (connection_id);

CREATE TABLE bank_transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES bank_accounts (id) ON DELETE CASCADE,
    plaid_transaction_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    merchant_name VARCHAR(255),
    amount_cents BIGINT NOT NULL,
    iso_currency_code VARCHAR(10),
    authorized_date DATE,
    posted_date DATE NOT NULL,
    pending BOOLEAN NOT NULL,
    removed BOOLEAN NOT NULL DEFAULT FALSE,
    expense_id UUID REFERENCES expenses (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX bank_transactions_account_id_idx ON bank_transactions (account_id);
CREATE INDEX bank_transactions_expense_id_idx ON bank_transactions (expense_id);
CREATE INDEX bank_transactions_posted_date_idx ON bank_transactions (posted_date DESC);
