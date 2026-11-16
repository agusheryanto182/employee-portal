# Project Plan

## Brief

The assignment from the mentor, for the "research, preparation and test" phase of a Full Stack Developer internship:

> Build a mock **dashboard** and its **backoffice**.
> - The only entry point is **sign in**. Employees are registered through the backoffice.
> - Every sign in is recorded in a **login history** per user.
> - Besides that: **logout** and **profile edit**.
> - Tech stack and time are free. Using AI is allowed.
> - Optional challenge: make it a **PWA** so it can be used on mobile.
> - Prepare a **presentation** explaining the thought process.

The goal is to show how far and how fast the work can go, so the process, the reasoning and the quality are judged as much as the features.

## Interpretation

| Area | Users | What it does |
|---|---|---|
| **Backoffice** (`/admin`) | Admin only | Register employees, manage their accounts, view everyone's login history, manage own account (profile, password), log out |
| **Dashboard** (`/`) | Employee only | View own login history, edit profile, log out |

Each area has its own sign-in page: employees at `/login`, admins at `/admin/login`. Each page only accepts its own role, enforced by the API, and neither role can open the other area. See [ADR 0012](decisions/0012-separate-admin-and-employee-areas.md).

The stack follows the internship job description (React or Angular, Java/Spring Boot, REST API, database, Git/GitHub with pull requests, unit and integration testing, Agile) so the project shows the skills the role needs. See [ADR 0001](decisions/0001-tech-stack.md).

## Scope

### In scope
- Email + password sign in on two separate pages, `/login` for employees and `/admin/login` for admins, each accepting only its own role ([ADR 0012](decisions/0012-separate-admin-and-employee-areas.md))
- Short-lived access token with a rotating refresh token ([ADR 0003](decisions/0003-jwt-with-refresh-cookie.md))
- Login history for every attempt, successful or not ([ADR 0005](decisions/0005-login-history-includes-failures.md))
- Backoffice: create, list/search, edit, deactivate/activate users, resend invite, trigger password reset
- Onboarding by invite email: employees set their own password ([ADR 0004](decisions/0004-invite-link-onboarding.md))
- Profile edit: full name, phone, change password. Only an admin can change an email.
- First admin seeded from environment variables
- Installable PWA with a mobile-first layout
- Unit, integration and end-to-end tests, CI on every pull request
- Free-tier production deployment ([ADR 0007](decisions/0007-free-tier-deployment.md))

### Out of scope (for now)
- Profile photo upload ([ADR 0008](decisions/0008-no-profile-photo.md))
- Hard delete of users ([ADR 0006](decisions/0006-no-hard-delete.md))
- Self registration, social login, multi-factor authentication
- Multiple admin roles or fine-grained permissions

## Way of working

- Work is split into sprints, each tracked as a **GitHub milestone** with one **issue** per unit of work and acceptance criteria.
- Every issue is built on its own branch (`feat/...`, `docs/...`, `chore/...`) and merged through a **pull request** that closes it (`Closes #n`).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org/).
- CI must pass before merging.
- Decisions are recorded as ADRs in [decisions/](decisions/) when they are made.

## Milestones

| Sprint | Goal | Issues |
|---|---|---|
| 0 | Setup & docs | #1 documentation, #2 repo scaffold, docker-compose, CI |
| 1 | Auth core (backend) | #3 schema & admin seed, #4 login/refresh/logout, #5 login history |
| 2 | User management & invites (backend) | #6 admin user API, #7 invite & reset tokens with email |
| 3 | Self-service API (backend) | #8 profile, password, own history |
| 4 | Employee area (frontend) | #9 auth pages, #10 dashboard, history, profile |
| 5 | Backoffice (frontend) | #11 users and login history UI |
| 6 | PWA & polish | #12 installable PWA |
| 7 | E2E & deployment | #13 Playwright, #14 Neon/Render/Brevo/Vercel |
| 8 | Final docs & presentation | #15 README, docs, presentation material |

Backend comes before frontend so the API contract is stable and tested before the UI uses it.

## Definition of done

An issue is done when:
- All acceptance criteria are met
- Tests are written and pass locally and in CI
- Docs and ADRs are updated if behaviour or a decision changed
- The change is merged through a pull request that closes the issue

The project is done when the full flow works in production: an admin signs in to the backoffice and invites an employee, the employee receives the email, activates the account, signs in to the dashboard, sees the login history, edits the profile and logs out, on desktop and as an installed PWA on a phone.
