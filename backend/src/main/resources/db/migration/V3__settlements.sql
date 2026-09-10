ALTER TABLE expenses ADD COLUMN created_by UUID;
UPDATE expenses SET created_by = paid_by WHERE created_by IS NULL;
ALTER TABLE expenses ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE expenses
    ADD CONSTRAINT expenses_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT;
CREATE INDEX expenses_created_by_idx ON expenses (created_by);

CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES groups (id) ON DELETE RESTRICT,
    from_user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    to_user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT settlements_distinct_users_check CHECK (from_user_id <> to_user_id)
);

CREATE INDEX settlements_group_id_created_at_idx ON settlements (group_id, created_at);
CREATE INDEX settlements_from_user_id_idx ON settlements (from_user_id);
CREATE INDEX settlements_to_user_id_idx ON settlements (to_user_id);
