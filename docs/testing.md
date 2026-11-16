# Testing

## Strategy

```
          ┌──────────────┐
          │  E2E (few)   │  Playwright: full user journeys in a real browser
          ├──────────────┤
          │ Integration  │  Spring Boot + real PostgreSQL + fake SMTP
          ├──────────────┤
          │  Unit (many) │  Services and components in isolation
          └──────────────┘
```

Most checks are fast unit tests. Integration tests prove the layers work together against a real database. A few end-to-end tests prove the main journey works for a user.

## Backend

| Level | Tools | What is covered |
|---|---|---|
| Unit | JUnit 5, Mockito, AssertJ | Auth service (success, wrong password, pending/inactive user, unknown email), token service (expiry, reuse, hashing), login history recording, rate limiter |
| Integration | `@SpringBootTest`, MockMvc, **Testcontainers** (PostgreSQL), **GreenMail** (in-memory SMTP) | HTTP endpoints, security rules (employee gets 403 on `/api/admin/**`), Flyway migrations, the invite → activate → login → history flow, emails actually sent |

Testcontainers runs a real PostgreSQL in Docker, so queries and migrations are tested on the same database used in production instead of H2.

```bash
cd backend
./mvnw test      # unit tests
./mvnw verify    # unit + integration tests (needs Docker)
```

## Frontend

| Level | Tools | What is covered |
|---|---|---|
| Unit / component | Vitest, Testing Library, MSW | Form validation, error states, route guards, token refresh in the API client |

```bash
cd frontend
npm test
```

## End to end

Playwright runs against the full Docker Compose stack. The invite link is read from the **Mailpit API**, so the test follows the real email path.

Main journey:
1. Admin signs in and creates an employee
2. The test reads the invite email from Mailpit and opens the link
3. The employee sets a password and signs in
4. A failed login and a successful login both appear in the history
5. The employee edits the profile and logs out
6. A protected page redirects to the login page after logout

```bash
docker compose up -d
cd e2e && npx playwright test
```

## Continuous integration

GitHub Actions runs on every pull request and on pushes to `master`:
- Backend: `./mvnw verify` (GitHub-hosted runners include Docker for Testcontainers)
- Frontend: lint, type check, tests, production build
- E2E: added in Sprint 7

A pull request is merged only when CI is green.
