ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS lock_until TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS ix_users_lock_until
    ON users(lock_until)
    WHERE lock_until IS NOT NULL;
