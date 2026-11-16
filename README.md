# Employee Portal

A mock employee **dashboard** and **backoffice**: admins register employees, employees sign in, see their login history, edit their profile and log out. Installable as a PWA.

> Status: Sprint 0 (setup). See the [project plan](docs/project-plan.md) and the [issues](https://github.com/agusheryanto182/employee-portal/issues).

## Stack

- **Backend:** Java 25 (LTS), Spring Boot 4.1, Spring Security, Data JPA, Flyway, PostgreSQL
- **Frontend:** React 19, Vite, TypeScript, Tailwind CSS
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

Health check: http://localhost:8080/actuator/health

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
