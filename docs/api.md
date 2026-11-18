# API

Base path: `/api`. JSON in and out. Interactive documentation is served by Swagger UI at `/swagger-ui.html` when the backend runs.

## Authentication

- Employees sign in with `POST /api/auth/login`, admins with `POST /api/admin/auth/login`. Each endpoint only accepts its own role ([ADR 0012](decisions/0012-separate-admin-and-employee-areas.md)). Both return an **access token** (JWT, 15 minutes) in the response body and set a **refresh token** (7 days) in an httpOnly cookie scoped to `/api/auth`.
- Protected endpoints require `Authorization: Bearer <access token>`.
- When the access token expires (HTTP 401), the client calls `POST /api/auth/refresh`, which uses the cookie and returns a new access token.
- See [auth-flows.md](auth-flows.md) and [ADR 0003](decisions/0003-jwt-with-refresh-cookie.md).

## Endpoints

### Auth (public)

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Employee sign in. `{ email, password }` → `{ accessToken, tokenType, expiresIn, user }` and sets the refresh cookie. Every attempt is written to login history. |
| POST | `/api/admin/auth/login` | Admin sign in. Same request and response. An account of the other role gets the same generic 401 (recorded as `WRONG_PORTAL`). |
| POST | `/api/auth/refresh` | Rotates the refresh cookie and returns `{ accessToken, user }`. Shared by both roles. |
| POST | `/api/auth/logout` | Revokes the refresh token and clears the cookie. Shared by both roles. |
| GET | `/api/auth/tokens/{token}` | Checks an invite or reset link before showing the form → `{ type, email, fullName }` |
| POST | `/api/auth/activate` | `{ token, password }` sets the first password and activates the account → 204 |
| POST | `/api/auth/reset-password` | `{ token, password }` sets a new password and revokes all sessions → 204 |

Passwords must be 8 to 72 characters (bcrypt ignores anything after 72 bytes). An unknown, expired, used or replaced link, or one used for the wrong purpose, always gets the same 400 "This link is invalid or has expired".

### Self-service (any signed-in user, both roles)

Employees use these from the dashboard, admins from **My account** in the backoffice. They also check that the account is still `ACTIVE`, so a deactivated user's remaining access token (up to 15 minutes) gets 401 here.

| Method | Path | Description |
|---|---|---|
| GET | `/api/me` | Current user's profile |
| PATCH | `/api/me` | Any of `{ fullName, phone }`. Omitted or `null` fields are unchanged; `"phone": ""` removes the phone. Other fields such as `email` or `role` are ignored: only an admin can change them. |
| PUT | `/api/me/password` | `{ currentPassword, newPassword }`. Signs out every device, then returns a new session for this one (same body as login, plus a new refresh cookie). Also cancels any unused reset link. |
| GET | `/api/me/login-history?page=&size=` | Own login attempts (successful and failed, with IP and user agent), newest first |

A wrong current password, or a new password equal to the current one, returns 400 with the field in `errors` (for example `{ "currentPassword": "is incorrect" }`), not 401, so the client does not mistake it for an expired session.

The password change signs out all sessions and issues a new one instead of keeping "the current session", because the refresh cookie is scoped to `/api/auth` and is not sent to `/api/me/password`.

### Backoffice (`ADMIN` only)

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/users?search=&status=&role=&page=&size=` | List users, newest first. `search` matches part of the email or full name, case-insensitive. |
| POST | `/api/admin/users` | `{ email, fullName, phone?, role }` creates a `PENDING` user (201 with `Location`) and emails an invite link |
| GET | `/api/admin/users/{id}` | User detail |
| PATCH | `/api/admin/users/{id}` | Any of `{ email, fullName, phone, role }`. Omitted or `null` fields are unchanged; `"phone": ""` removes the phone. Changing the role revokes the user's sessions. Changing the email cancels links sent to the old address, and a `PENDING` user gets a new invite at the new one. |
| POST | `/api/admin/users/{id}/deactivate` | Sets `INACTIVE`, revokes all sessions and cancels unused links. Works for `ACTIVE` and `PENDING` users. |
| POST | `/api/admin/users/{id}/activate` | Reactivates an `INACTIVE` user: `ACTIVE`, or back to `PENDING` if they never set a password |
| POST | `/api/admin/users/{id}/resend-invite` | Emails a new invite to a `PENDING` user (older links stop working) → `{ link }`. 409 for other statuses. |
| POST | `/api/admin/users/{id}/reset-password` | Emails a reset link to an `ACTIVE` user → `{ link }`. 409 for other statuses. |
| GET | `/api/admin/login-history?userId=&success=&from=&to=&page=&size=` | All login attempts, newest first. `from` (inclusive) and `to` (exclusive) are ISO-8601 instants such as `2026-11-18T00:00:00Z`. |

User responses look like `{ id, email, fullName, phone, role, status, createdAt, updatedAt }`. Login history entries look like `{ id, userId, email, success, failureReason, ipAddress, userAgent, createdAt }`, where `userId` is `null` for an email that matches no account.

An admin cannot deactivate their own account or change their own role (409). This keeps at least one active admin. Deactivating an already inactive user, or activating a user who is not `INACTIVE`, changes nothing and returns the user.

The `{ link }` returned by the resend and reset endpoints lets the admin copy the link and share it another way if the email does not arrive ([ADR 0004](decisions/0004-invite-link-onboarding.md)).

## Pagination

List endpoints accept `page` (0-based) and `size` (default 20, 1 to 100; other values return 400) and return:

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
```

## Errors

Every error uses the RFC 9457 Problem Details format:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": { "email": "must be a well-formed email address" }
}
```

| Status | When |
|---|---|
| 400 | Validation failed, or an invite or reset link is invalid, expired or used |
| 401 | Not signed in, token expired, or wrong credentials (the message is always "Invalid email or password") |
| 403 | Signed in but not allowed (for example an employee calling `/api/admin/**`) |
| 404 | Resource not found |
| 409 | Conflict: an email that is already registered, an admin deactivating themselves or changing their own role, or an invite or reset for a user in the wrong status |
| 429 | Too many failed logins: 5 failures per IP + email within 15 minutes. The response has a `Retry-After` header (seconds). |
