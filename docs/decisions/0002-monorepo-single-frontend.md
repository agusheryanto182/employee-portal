# 0002. Monorepo with a single role-based frontend

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
The brief asks for a "dashboard" (employees) and a "backoffice" (admins). They share the same users, the same sign-in and the same API.

## Decision
- One repository containing `backend/`, `frontend/`, `e2e/` and `docs/`.
- One React app with two areas: `/admin/*` for the backoffice (admins only) and `/` for the employee dashboard (employees only). Routes are guarded by role, and the API enforces the same rules on the server side.

## Alternatives considered
- **Two separate frontend apps** (dashboard and backoffice): stronger isolation and separate deployments, but duplicated auth, layout and API code, and twice the build and deploy work for a small project.
- **Separate repositories for backend and frontend:** harder to keep the API and UI changes in one pull request, and harder for a reviewer to see the whole project.

## Consequences
- One pull request can change the API, the UI, the tests and the docs together.
- Two sign-in pages (`/login` and `/admin/login`) in the same app, sharing the auth code; each role is kept in its own area ([ADR 0012](0012-separate-admin-and-employee-areas.md)).
- Employee bundles also contain the admin screens' code unless routes are lazy-loaded, so admin routes are code-split. Security never depends on the frontend: the backend checks the role on every `/api/admin/**` request.
- If the backoffice later needs to be separated (for example on an internal network), the shared code can be extracted into a package.
