# 0012. Admins and employees each have their own area

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
The brief describes two parts: a **dashboard** for employees and a **backoffice** where employees are registered. The first plan let admins use both. That mixes two different roles: an employee manages only their own account, while an admin manages other people's accounts.

## Decision
Each role has exactly one area:

| Role | Area | Sign-in page | Sign-in API | Opening the other area |
|---|---|---|---|---|
| `EMPLOYEE` | Dashboard `/` (own history, profile, log out) | `/login` | `POST /api/auth/login` | `/admin/*` → redirected to `/`; `/api/admin/**` → 403 |
| `ADMIN` | Backoffice `/admin` (users, all login history, **My account**, log out) | `/admin/login` | `POST /api/admin/auth/login` | `/` → redirected to `/admin` |

- **Separate sign-in pages, enforced by the API.** Each login endpoint only accepts its own role. An account signing in on the other page gets the same generic "Invalid email or password" response, and the attempt is recorded with the reason `WRONG_PORTAL`. The separation is therefore real, not only visual.
- Refresh and logout stay shared (`/api/auth/refresh`, `/api/auth/logout`); the role comes from the account.

- Admins edit their own profile and password from **My account** inside the backoffice.
- Both areas use the same self-service API (`/api/me/**`), so no endpoint is duplicated.
- An admin's own login attempts appear in the backoffice's login history, like everyone else's.

## Alternatives considered
- **Admins can use both areas** (the original plan): one more screen for admins with no purpose in the brief, and it blurs the line between the two roles.
- **One shared sign-in page that redirects by role:** one form less, but the backoffice entrance is the same as the employees' and the two areas feel like one app.
- **Separate pages that both call the same endpoint:** looks separate, but an admin could still sign in through the employee page; the separation would only be cosmetic.

## Consequences
- Each role sees only what it needs.
- The frontend needs two login pages and role-based redirects in both directions.
- A user who picks the wrong page sees the generic error; the login pages link to each other to help.
- If admins later need employee features (for example if admins are also staff members), this ADR would be superseded.
