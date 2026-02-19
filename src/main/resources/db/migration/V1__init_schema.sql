CREATE TABLE accounts (
    user_id BIGINT PRIMARY KEY,
    balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0)
);

CREATE TABLE operations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES accounts(user_id),
    operation_type VARCHAR(32) NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL,
    related_user_id BIGINT
);

CREATE INDEX idx_operations_user_created_at ON operations (user_id, created_at DESC);
