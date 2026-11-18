-- An invited user who never activated (PENDING, no password) can now be deactivated.
-- Only ACTIVE accounts need a password; reactivating a user without one returns it to PENDING.
ALTER TABLE users DROP CONSTRAINT ck_users_active_has_password;
ALTER TABLE users ADD CONSTRAINT ck_users_active_has_password
    CHECK (status <> 'ACTIVE' OR password_hash IS NOT NULL);
