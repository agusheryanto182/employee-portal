# Authentication Flows

Token storage on the client:
- **Access token**: kept in memory (a JavaScript variable), never in `localStorage`, so an XSS bug cannot read a long-lived token from storage.
- **Refresh token**: httpOnly, `Secure`, `SameSite=Lax` cookie with path `/api/auth`. JavaScript cannot read it, and it is only sent to the auth endpoints.

See [ADR 0003](decisions/0003-jwt-with-refresh-cookie.md) for the reasoning.

## Login

```mermaid
sequenceDiagram
    actor U as User
    participant FE as React app
    participant API as Spring Boot API
    participant DB as PostgreSQL

    U->>FE: email + password
    FE->>API: POST /api/auth/login
    API->>API: rate limit check (IP + email)
    API->>DB: find user by email (lowercase)
    alt unknown email, wrong password, PENDING or INACTIVE
        API->>DB: insert login_history (success = false, reason)
        API-->>FE: 401 "Invalid email or password"
    else valid
        API->>DB: insert refresh_tokens (hash)
        API->>DB: insert login_history (success = true, IP, user agent)
        API-->>FE: 200 { accessToken, user } + Set-Cookie refresh_token
        FE->>FE: keep access token in memory
    end
```

The response for a failed login is the same whatever the reason, so the endpoint cannot be used to discover which emails exist. The real reason is stored for the admin.

## Calling the API and refreshing

```mermaid
sequenceDiagram
    participant FE as React app
    participant API as Spring Boot API
    participant DB as PostgreSQL

    FE->>API: GET /api/me (Bearer access token)
    API-->>FE: 401 (access token expired)
    FE->>API: POST /api/auth/refresh (cookie)
    API->>DB: find refresh token by hash
    alt valid and not revoked
        API->>DB: revoke old token, insert new token
        API-->>FE: 200 { accessToken } + new cookie
        FE->>API: retry GET /api/me
    else revoked token reused within 30 s (another tab won the race)
        API-->>FE: 401, FE retries once with the new cookie
    else revoked token reused later (possible theft)
        API->>DB: revoke ALL tokens of that user
        API-->>FE: 401
        FE->>FE: redirect to /login
    end
```

On page reload the in-memory access token is gone, so the app calls `/api/auth/refresh` once at startup to restore the session. Several requests failing at the same time share one refresh call.

## Logout

```mermaid
sequenceDiagram
    participant FE as React app
    participant API as Spring Boot API
    participant DB as PostgreSQL

    FE->>API: POST /api/auth/logout (cookie)
    API->>DB: set revoked_at on the refresh token
    API-->>FE: 204 + expired cookie
    FE->>FE: clear access token and cached data, go to /login
```

## Invite and activation

```mermaid
sequenceDiagram
    actor A as Admin
    actor E as Employee
    participant FE as React app
    participant API as Spring Boot API
    participant DB as PostgreSQL
    participant M as SMTP (Mailpit / Brevo)

    A->>FE: create employee (email, name, role)
    FE->>API: POST /api/admin/users
    API->>DB: insert user (PENDING, no password)
    API->>DB: insert account_tokens (INVITE, SHA-256 hash, expires in 24h)
    API-->>FE: 201 user
    API-)M: after commit, async: send email with /activate?token=...
    M-)E: invite email
    E->>FE: open link
    FE->>API: GET /api/auth/tokens/{token}
    API-->>FE: { type: INVITE, email, fullName }
    E->>FE: choose password
    FE->>API: POST /api/auth/activate { token, password }
    API->>DB: set password_hash (bcrypt), status ACTIVE, mark token used
    API-->>FE: 200
    FE->>FE: go to /login
```

If the email does not arrive, the admin clicks **Resend invite**: a new token is issued, older ones are invalidated, and the link is shown so it can be copied.

## Password reset (admin-triggered)

Same flow as the invite, with type `RESET`, a 1-hour expiry, and `POST /api/auth/reset-password`. A successful reset revokes every refresh token of the user, signing out all devices.
