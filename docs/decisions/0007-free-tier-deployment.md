# 0007. Free-tier deployment on Vercel, Render and Neon

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
The project should be reachable online so the mentor can try it, including installing the PWA on a phone, at no cost.

## Decision
| Component | Service |
|---|---|
| Frontend | **Vercel** (static build) |
| Backend | **Render** free web service, Docker image |
| Database | **Neon** free PostgreSQL |
| Email | **Brevo** free SMTP |

The frontend's `vercel.json` rewrites `/api/:path*` to the Render backend. The browser only sees the Vercel domain, so the refresh-token cookie is first-party.

## Alternatives considered
- **Railway, Fly.io:** no longer have a free tier for new users.
- **Koyeb free instance:** possible, but tighter memory limits for a JVM app.
- **Frontend and backend on separate domains with CORS:** the refresh cookie would be a third-party cookie (`*.vercel.app` → `*.onrender.com`), which modern browsers block or restrict.
- **Serving the React build from Spring Boot:** one service, but loses Vercel's CDN and preview deployments, and couples the two builds.

## Consequences
- Zero hosting cost.
- **Cold starts:** Render's free service sleeps after 15 minutes idle; the first request then takes about 30–60 seconds. This is documented in the README, and the service is woken up before a demo.
- Render's free tier has 512 MB of RAM, so the JVM is tuned (`-XX:MaxRAMPercentage=75`) and the app stays lean.
- Neon also scales to zero, adding a short delay to the first query after idle.
- Vercel proxying adds a small latency hop on each API call.
