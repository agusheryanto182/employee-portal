# 0006. Deactivate users instead of deleting them

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
Admins need to remove access for employees who leave. Login history references users, and deleting a user would either delete or orphan that history.

## Decision
No hard delete. Admins set a user to `INACTIVE`, which:
- blocks sign in (recorded as a failed attempt with reason `ACCOUNT_INACTIVE`),
- revokes all of the user's refresh tokens immediately.

Admins can reactivate an inactive user.

## Alternatives considered
- **Hard delete with cascade:** removes the audit trail, which defeats the purpose of the login history.
- **Soft delete with a `deleted_at` column:** similar effect, but a separate flag next to `status` creates two sources of truth.

## Consequences
- The audit trail stays complete.
- Returning employees can be reactivated without re-creating the account.
- An email address stays taken by an inactive account. If needed, the admin can edit the old account's email first.
- Personal data is kept indefinitely. A real deployment would need a data-retention or anonymisation process; that is out of scope here.
