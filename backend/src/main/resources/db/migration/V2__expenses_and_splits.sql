CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES groups (id) ON DELETE RESTRICT,
    paid_by UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    description VARCHAR(255) NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    split_strategy VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT expenses_split_strategy_check CHECK (split_strategy IN ('EQUAL', 'EXACT', 'PERCENTAGE'))
);

CREATE TABLE expense_splits (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    share_cents BIGINT NOT NULL CHECK (share_cents >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT expense_splits_expense_user_unique UNIQUE (expense_id, user_id)
);

CREATE INDEX expenses_group_id_created_at_idx ON expenses (group_id, created_at DESC);
CREATE INDEX expenses_paid_by_idx ON expenses (paid_by);
CREATE INDEX expense_splits_expense_id_idx ON expense_splits (expense_id);
CREATE INDEX expense_splits_user_id_idx ON expense_splits (user_id);
