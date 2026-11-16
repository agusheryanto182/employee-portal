# 0011. Vite SPA with TanStack Router instead of Next.js

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
React's official docs recommend starting new apps with a framework (Next.js, React Router v7), and also describe building an SPA with Vite, a router and a data-fetching library when that fits better. The question was whether Next.js would be simpler, especially for routing.

This app:
- is entirely behind a login, so there is no SEO or public-page rendering need,
- already has its own backend (Spring Boot, [ADR 0001](0001-tech-stack.md)),
- keeps the access token in memory on the client ([ADR 0003](0003-jwt-with-refresh-cookie.md)),
- must work as an installable PWA.

## Decision
Build the frontend as a **single-page app with Vite** and **TanStack Router** using **file-based routing**:
- Routes and nested layouts come from the `src/routes/` folder structure, as in Next.js.
- Links and route params are type-checked at compile time.
- Auth and role guards run in each route's `beforeLoad`.
- TanStack Query handles server data; route loaders can prefetch through it.

## Alternatives considered
- **Next.js:** file-based routing and server rendering, but its server features (server components, API routes, server actions) duplicate the Spring Boot backend and add a Node server in the middle. Route protection in Next.js runs on the server and can only read cookies, which conflicts with the in-memory access token design. Its SEO benefits do not apply behind a login.
- **React Router v7:** a solid choice with similar features; TanStack Router was preferred for its stronger type safety and its integration with TanStack Query.

## Consequences
- Routing feels like Next.js (folders = routes, nested layouts) without an extra server.
- The build is static files: simple to deploy on Vercel and to turn into a PWA.
- No server rendering: the first load shows a loading state while the session is restored. This is acceptable for an internal dashboard.
- If public, SEO-relevant pages are ever needed, they would be a separate site or a move to a framework.
