# 0013. Backend packages by feature, layered inside

- **Status:** Accepted
- **Date:** 2026-11-18

## Context
The backend grows with every sprint (auth, users, login history, mail, self-service). The code needs a structure that keeps a feature easy to find and change, and that a reviewer can follow without a tour.

## Decision
- **Top-level packages are features:** `auth`, `user`, `loginhistory`, `mail`, plus `common` (errors, pagination, small utilities) and `config` (security, OpenAPI, seeding).
- **Inside a feature, classes follow three layers**, named by suffix rather than sub-packages:
  - `*Controller`: HTTP only. Reads the request, calls one service method, returns a DTO. No repository access and no business rules.
  - `*Service`: business rules and transactions (`@Transactional` lives here).
  - `*Repository`: Spring Data interfaces. Only services use them.
- **Entities stay inside the service layer.** Controllers receive and return records (`*Request`, `*Response`), so the API contract does not change when a column does, and fields such as `password_hash` cannot leak.
- **Features talk through services.** For example `UserAdminService` calls `RefreshTokenService.revokeAll` instead of using the refresh token repository.
- **Admin endpoints live with their feature** (`user/AdminUserController`, `loginhistory/AdminLoginHistoryController`), not in an `admin` package. The `/api/admin/**` path, not the package, is what security protects.

## Alternatives considered
- **Package by layer** (`controller/`, `service/`, `repository/`, `entity/`): common in tutorials, but one feature is spread over four folders and every folder grows without limit.
- **Hexagonal / clean architecture** (ports, adapters, separate domain model): stronger isolation from the framework, but many more classes and mappings than a CRUD-heavy project of this size needs.
- **An `admin` feature package:** groups the backoffice API, but splits user rules between `user` and `admin`.
- **Enforcing the rules with ArchUnit tests:** possible later. For now the rules are small enough to check in review.

## Consequences
- Adding a feature means adding one package; removing it means deleting one package.
- The layer rules are conventions, checked in review rather than by the compiler.
- Package-private visibility can hide helpers inside a feature (for example `UserSpecifications`).
