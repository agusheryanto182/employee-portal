# Employee Portal

A mock employee **dashboard** and **backoffice**, each with its own sign-in page:

- **Backoffice** (`/admin`, admins only): register and manage employees, view everyone's login history, manage own account
- **Dashboard** (`/`, employees only): view own login history, edit profile, log out

Installable as a PWA.

> Status: Sprint 1 (backend auth core). See the [project plan](docs/project-plan.md) and the [issues](https://github.com/agusheryanto182/employee-portal/issues).

## Stack

- **Backend:** Java 25 (LTS), Spring Boot 4.1, Spring Security, Data JPA, Flyway, PostgreSQL
- **Frontend:** React 19, Vite, TypeScript, TanStack Router and Query, Tailwind CSS, shadcn/ui, react-hook-form + zod
- **Tests:** JUnit 5, Testcontainers, Vitest, Testing Library (Playwright later)
- **CI:** GitHub Actions

Why these choices: [docs/decisions](docs/decisions/).

## Run locally

Requirements: JDK 25, Node 26, Docker. With SDKMAN: `cd backend && sdk env install`.

```bash
# 1. Database (PostgreSQL :5432) and fake SMTP inbox (Mailpit UI :8025)
docker compose up -d

# 2. Backend API on http://localhost:8080
cd backend
./mvnw spring-boot:run

# 3. Frontend on http://localhost:5173 (proxies /api to the backend)
cd frontend
npm install
npm run dev
```

`./mvnw spring-boot:run` uses the `local` profile ([application-local.yml](backend/src/main/resources/application-local.yml)), which seeds an admin (signs in at `/admin/login`, API `POST /api/admin/auth/login`):

| Email | Password |
|---|---|
| `admin@example.com` | `Admin12345!` |

- API docs (Swagger UI): http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health
- Emails: http://localhost:8025 (Mailpit)

## Tests

```bash
cd backend && ./mvnw verify   # unit + integration tests (needs Docker)
cd frontend && npm test       # component tests
```

## Documentation

| | |
|---|---|
| [Project plan](docs/project-plan.md) | Brief, scope, milestones |
| [Architecture](docs/architecture.md) | System overview and tech stack |
| [Database](docs/database.md) | ERD |
| [API](docs/api.md) | Endpoints |
| [Auth flows](docs/auth-flows.md) | Sequence diagrams |
| [Testing](docs/testing.md) | Test strategy |
| [Deployment](docs/deployment.md) | Free-tier hosting |
