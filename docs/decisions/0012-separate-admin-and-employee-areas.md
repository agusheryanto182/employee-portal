# 0012. Admins and employees each have their own area

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
The brief describes two parts: a **dashboard** for employees and a **backoffice** where employees are registered. The first plan let admins use both. That mixes two different roles: an employee manages only their own account, while an admin manages other people's accounts.

## Decision
Each role has exactly one area:

| Role | Area | After sign in | Opening the other area |
|---|---|---|---|
| `EMPLOYEE` | Dashboard `/` (own history, profile, log out) | `/` | `/admin/*` → redirected to `/`; `/api/admin/**` → 403 |
| `ADMIN` | Backoffice `/admin` (users, all login history, **My account**, log out) | `/admin` | `/` → redirected to `/admin` |

- Admins edit their own profile and password from **My account** inside the backoffice.
- Both areas use the same self-service API (`/api/me/**`), so no endpoint is duplicated. The separation is a navigation rule in the frontend plus role checks on `/api/admin/**` in the backend.
- An admin's own login attempts appear in the backoffice's login history, like everyone else's.

## Alternatives considered
- **Admins can use both areas** (the original plan): one more screen for admins with no purpose in the brief, and it blurs the line between the two roles.
- **Separate login pages per area:** clearer URLs, but two forms for the same credentials; the role already tells where to go.

## Consequences
- Each role sees only what it needs.
- The frontend needs role-based redirects in both directions, not just "employees cannot open `/admin`".
- If admins later need employee features (for example if admins are also staff members), this ADR would be superseded.
