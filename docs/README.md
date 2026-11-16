# Documentation

This folder records **what** is being built, **how** it is built and **why** each decision was made.
It is written before the code and updated whenever a decision changes.

| Document | Contents |
|---|---|
| [project-plan.md](project-plan.md) | Original brief, scope, milestones and definition of done |
| [architecture.md](architecture.md) | System overview, tech stack rationale, repository layout, deployment |
| [database.md](database.md) | Entity relationship diagram and table descriptions |
| [api.md](api.md) | REST endpoints, authentication rules and error format |
| [auth-flows.md](auth-flows.md) | Sequence diagrams for login, refresh, logout, invite and password reset |
| [testing.md](testing.md) | Testing strategy and how to run each test suite |
| [deployment.md](deployment.md) | Free-tier production setup and environment variables |
| [decisions/](decisions/) | Architecture Decision Records (ADRs) |

## Decision records

| # | Decision | Status |
|---|---|---|
| [0001](decisions/0001-tech-stack.md) | React + Spring Boot + PostgreSQL | Accepted |
| [0002](decisions/0002-monorepo-single-frontend.md) | Monorepo with a single role-based frontend | Accepted |
| [0003](decisions/0003-jwt-with-refresh-cookie.md) | Short-lived JWT + rotating refresh token in an httpOnly cookie | Accepted |
| [0004](decisions/0004-invite-link-onboarding.md) | Invite link onboarding instead of emailing passwords | Accepted |
| [0005](decisions/0005-login-history-includes-failures.md) | Login history records failed attempts too | Accepted |
| [0006](decisions/0006-no-hard-delete.md) | Deactivate users instead of deleting them | Accepted |
| [0007](decisions/0007-free-tier-deployment.md) | Free-tier deployment on Vercel, Render and Neon | Accepted |
| [0008](decisions/0008-no-profile-photo.md) | No profile photo upload in the first version | Accepted |
| [0009](decisions/0009-maven-wrapper.md) | Maven with the Maven Wrapper | Accepted |
| [0010](decisions/0010-oxlint.md) | oxlint for frontend linting | Accepted |
| [0011](decisions/0011-vite-spa-tanstack-router.md) | Vite SPA with TanStack Router instead of Next.js | Accepted |
| [0012](decisions/0012-separate-admin-and-employee-areas.md) | Admins and employees each have their own area | Accepted |

New ADRs copy [decisions/template.md](decisions/template.md).
