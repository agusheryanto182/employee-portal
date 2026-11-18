# 0014. PWA: cache the app shell, never the API

- **Status:** Accepted
- **Date:** 2026-11-18

## Context
The app should be installable on phones and open without a connection (issue #12). Everything a user sees is personal data behind a sign-in: login history, profiles and, in the backoffice, every user's details. A shared device or a signed-out session must never show cached data from someone else.

## Decision
- `vite-plugin-pwa` generates a Workbox service worker that **precaches only the built app shell** (HTML, JS, CSS, fonts, icons). Every route falls back to the cached `index.html`.
- **No API response is cached.** There is no runtime caching rule, and navigations to `/api/*` are excluded from the fallback. The access token stays in memory and the refresh token in an httpOnly cookie, so nothing personal ends up in Cache Storage.
- Offline, the shell still opens:
  - Pages already loaded stay on screen with an offline banner. TanStack Query pauses requests and retries them when the connection is back.
  - If the session cannot be checked (for example a reload while offline), the route guard shows an offline screen instead of the sign-in page. Back online, the page reloads by itself.
- New versions use `registerType: 'prompt'`: a toast offers **Reload**, so an update never discards a half-filled form.
- Icons are generated from one SVG (`public/logo.svg`) with `@vite-pwa/assets-generator`, including a maskable icon and an Apple touch icon.

## Alternatives considered
- **Cache API responses (stale-while-revalidate)** for real offline reading of the history. Rejected: it stores personal data on the device beyond the session and can show one user's data to the next. The app is useless offline anyway for its main tasks: signing in, editing and managing users.
- **`autoUpdate`** (reload as soon as a new version is installed). Simpler, but it could reload in the middle of editing a user.
- **A separate static `offline.html`.** Not needed: the SPA shell is cached, so the offline screen is a normal React screen with the same look.

## Consequences
- Installable on Android (Chrome's installability check passes) and iOS (Add to Home Screen, standalone display, Apple touch icon, safe-area padding for the bottom tab bar).
- Offline the app explains itself instead of failing, but shows no data that was not already on screen.
- `sw.js` must not be cached by the CDN, otherwise users keep old versions longer. The Vercel configuration in Sprint 7 sets `Cache-Control: no-cache` for it (see deployment.md).
- Lighthouse removed its PWA category in version 12, so installability is checked with Chrome's own installability report and by installing the app.
