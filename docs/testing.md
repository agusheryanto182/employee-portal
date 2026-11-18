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
| Unit (`*Test`) | JUnit 5, Mockito, AssertJ | Auth service (success, wrong password, pending/inactive user, unknown email, rate limit), refresh token rotation (expiry, reuse, grace window), rate limiter |
| Integration (`*IT`) | `@SpringBootTest`, MockMvc, **Testcontainers** (PostgreSQL), **GreenMail** (in-memory SMTP) | HTTP endpoints, security rules (employee gets 403 on `/api/admin/**`, each login endpoint rejects the other role), Flyway migrations, the invite → activate → login → history flow, emails actually sent |

Testcontainers runs a real PostgreSQL in Docker, so queries and migrations are tested on the same database used in production instead of H2.

```bash
cd backend
./mvnw test      # unit tests (*Test, Surefire)
./mvnw verify    # unit + integration tests (*IT, Failsafe; needs Docker)
```

## Frontend

| Level | Tools | What is covered |
|---|---|---|
| Unit / component | Vitest, Testing Library, **MSW** (fake API at the network level) | API client: token in memory, a single refresh shared by concurrent 401s, no refresh for failed sign-ins, session expiry; route guards (redirect to the area's sign-in page, keep each role in its own area, no open redirects); sign-in form (validation, generic 401, 429 with wait time); activate/reset form (bad or wrong-type links, password confirmation); change password (field error, new session kept); login history list; backoffice: user list filters sent to the API and reset to page 1, create user (taken email shown at the field), edit sends only changed fields, own role locked, actions per status with confirmation before deactivating, copyable invite link, login history filters with local-day date ranges; session restore (one refresh when signed out, an expired session never comes back from the cache, retry after being offline); offline guard and banner |

MSW runs with `onUnhandledFrame: 'error'`, so a component calling an endpoint the test did not expect fails the test. Components that render links use `renderWithRouter` (`src/test/utils.tsx`), which mounts them in an in-memory router.

```bash
cd frontend
npm test
```

The service worker itself is checked by hand on a production build (`npm run build && npm run preview`): Chrome's installability report, a reload while offline, and no `/api` entries in Cache Storage.

## End to end

Playwright (`e2e/`) runs against the full Docker Compose stack: PostgreSQL, Mailpit, the backend image and the frontend image (nginx proxying `/api` like the Vercel rewrite). The invite link is read from the **Mailpit API**, so the test follows the real email path. The journey runs twice, on desktop Chrome and on a Pixel 7 viewport, each with its own new employee.

Main journey (`e2e/tests/main-journey.spec.ts`):
1. Admin signs in at `/admin/login` and adds an employee
2. The test reads the invite email from Mailpit and opens the link, sets a password, and checks that the link works only once
3. Signing in at `/admin/login` fails for the employee, a wrong password fails at `/login`, then the employee signs in
4. The history shows the successful sign-in and both failed attempts with their reasons; an employee opening `/admin` is redirected to `/`
5. The employee edits the profile (still saved after a reload) and signs out; a protected page then redirects to `/login`
6. The admin finds the employee, sees the failed attempts in their history, deactivates them, and the employee can no longer sign in

```bash
docker compose --profile app up -d --build --wait   # app on http://localhost:3000
cd e2e
npm install
npx playwright install chromium                     # or set CHROMIUM_PATH to an installed Chromium
npx playwright test
```

`E2E_BASE_URL` and `MAILPIT_URL` point the tests at another stack.

## Continuous integration

GitHub Actions runs on every pull request and on pushes to `master`:
- Backend: `./mvnw verify` (GitHub-hosted runners include Docker for Testcontainers)
- Frontend: lint, type check, tests, production build
- E2E: builds and starts the Docker Compose stack, then runs Playwright; the report and backend logs are kept when it fails

A pull request is merged only when CI is green.
