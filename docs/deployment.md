# Deployment

All services run on free tiers. See [ADR 0007](decisions/0007-free-tier-deployment.md) for the trade-offs.

| Component | Service | Notes |
|---|---|---|
| Frontend | Vercel | Static Vite build, `/api/*` rewritten to the backend |
| Backend | Render (free web service, Docker) | Sleeps after 15 minutes idle; first request takes about 30–60 s |
| Database | Neon (free PostgreSQL) | Serverless Postgres, scales to zero |
| Email | Brevo (free SMTP, 300 emails/day) | Only a verified sender address is needed, no domain |

## Local development

```bash
docker compose up -d                       # PostgreSQL :5432, Mailpit UI :8025 / SMTP :1025
cd backend && ./mvnw spring-boot:run       # API on :8080
cd frontend && npm install && npm run dev  # app on :5173, proxies /api to :8080
```

Emails sent locally appear in the Mailpit inbox at http://localhost:8025.

## Environment variables (backend)

| Variable | Example | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://host/db?sslmode=require` | JDBC URL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | | Database credentials |
| `JWT_SECRET` | 64+ random characters | Signs access tokens |
| `APP_BASE_URL` | `https://employee-portal.vercel.app` | Used to build invite and reset links |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` | | First admin, created only if it does not exist |
| `MAIL_HOST` / `MAIL_PORT` | `smtp-relay.brevo.com` / `587` | SMTP server |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | | SMTP credentials |
| `MAIL_FROM` | verified sender address | Email "From" |
| `COOKIE_SECURE` | `true` | `Secure` flag on the refresh cookie (false only for local HTTP) |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75` | Keeps the JVM within Render's 512 MB |

Secrets are set in each provider's dashboard and are never committed.

## Production setup

> Filled in during Sprint 7 (#14). Planned steps:

1. **Neon**: create a project and copy the connection string.
2. **Brevo**: create an account, verify the sender email and generate an SMTP key.
3. **Render**: new Web Service from the GitHub repo, root `backend/`, Docker runtime, set the environment variables above, health check `/actuator/health`.
4. **Vercel**: import the repo, root `frontend/`, add a rewrite from `/api/:path*` to the Render URL in `vercel.json`.
5. Smoke test: sign in as the seeded admin, invite a real email address, activate, sign in, check the history.
