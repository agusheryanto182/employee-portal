# 0008. No profile photo upload in the first version

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
"Profile edit" could include a profile photo. Uploads need file validation, size limits, storage that survives redeploys (Render's free disk is ephemeral), and an object storage service.

## Decision
Profile edit covers full name, phone number and password. The avatar shows the user's initials.

## Alternatives considered
- **Store images in PostgreSQL (`bytea`):** no extra service, but bloats the database and the free storage quota.
- **Object storage (Cloudinary, S3-compatible):** the right production approach, but another account, SDK and set of secrets for a feature outside the brief's core.

## Consequences
- Less scope and fewer external dependencies.
- The profile looks a little plainer.
- Can be added later behind the same `PATCH /api/me` design with an object-storage integration.
