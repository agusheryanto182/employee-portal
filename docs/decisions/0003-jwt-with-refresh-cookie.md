# 0003. Short-lived JWT with a rotating refresh token in an httpOnly cookie

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
The frontend is a single-page app (and a PWA) talking to a separate REST API. Sessions must:
- survive page reloads and reopening the installed app,
- be revocable on logout, deactivation and password change,
- not expose long-lived credentials to JavaScript (XSS risk).

## Decision
- **Access token:** JWT signed with HS256, valid for **15 minutes**, sent as `Authorization: Bearer`. Kept in memory on the client only.
- **Refresh token:** random 256-bit value, valid for **7 days**, sent in an `httpOnly`, `Secure`, `SameSite=Lax` cookie with path `/api/auth`. Stored server-side as a SHA-256 hash.
- **Rotation:** every refresh issues a new refresh token and revokes the old one. If a revoked token is used again, all of that user's sessions are revoked (reuse detection).
- **Passwords:** bcrypt.

## Alternatives considered
- **Server-side sessions (`JSESSIONID`):** simple and revocable, but stateful for every request and less common for decoupled SPAs and mobile clients.
- **Long-lived JWT in `localStorage`:** simple, but any XSS can steal it and it cannot be revoked before it expires.
- **Auth provider (Keycloak, Auth0, Supabase Auth):** production-grade, but hides the parts the assessment is meant to show and adds an external dependency.

## Consequences
- API requests are verified without a database lookup; only refresh touches the database.
- Revocation takes effect within 15 minutes at most for access tokens, immediately for refresh.
- The cookie must be first-party, so in production the frontend proxies `/api` through its own domain ([ADR 0007](0007-free-tier-deployment.md)).
- `SameSite=Lax` plus a JSON-only, non-GET refresh endpoint protects against CSRF without a separate CSRF token.
- The frontend needs a refresh-on-401 interceptor and a session restore call at startup.
