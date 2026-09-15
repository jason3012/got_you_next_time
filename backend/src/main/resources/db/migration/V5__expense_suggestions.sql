ALTER TABLE bank_transactions
    ADD COLUMN category VARCHAR(100);

CREATE TABLE expense_suggestions (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES bank_transactions (id) ON DELETE CASCADE,
    candidate_group_id UUID NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    confidence DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT expense_suggestions_transaction_group_unique UNIQUE (transaction_id, candidate_group_id),
    CONSTRAINT expense_suggestions_confidence_check CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT expense_suggestions_status_check CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED'))
);

CREATE INDEX expense_suggestions_transaction_idx ON expense_suggestions (transaction_id);
CREATE INDEX expense_suggestions_group_status_idx ON expense_suggestions (candidate_group_id, status);

CREATE TABLE expense_suggestion_reasons (
    suggestion_id UUID NOT NULL REFERENCES expense_suggestions (id) ON DELETE CASCADE,
    reason_order INTEGER NOT NULL,
    reason VARCHAR(500) NOT NULL,
    PRIMARY KEY (suggestion_id, reason_order)
);
