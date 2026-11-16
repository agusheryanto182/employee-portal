# 0001. React + Spring Boot + PostgreSQL

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
The brief leaves the tech stack free. The project is an assessment for a Full Stack Developer internship. The job description and internship syllabus mention:
- Front end: HTML, CSS, JavaScript, responsive design, React or Angular
- Back end: REST APIs, server-side programming with **Java/Spring Boot**, API development
- Database management and integration, integration with external services
- Git/GitHub with branching and pull requests, unit and integration testing, Agile/Scrum

The developer is comfortable with Java and Spring Boot from a bootcamp.

## Decision
- **Backend:** Java 25 + Spring Boot 4.1 as a REST API (Spring Security, Data JPA, Flyway).
- **Frontend:** React + Vite + TypeScript as a single-page app.
- **Database:** PostgreSQL.

Rule for versions: use the **latest LTS or latest stable** release of every tool. Java 25 is the current LTS (Java 27 is newer but non-LTS; the next LTS, 29, is planned for September 2027). Spring Boot 4.1 is the current stable line; upgrade to 4.2 once it is GA.

## Alternatives considered
- **Next.js full stack (TypeScript only):** fastest to build and deploy, but does not show Java/Spring Boot, which is the company's backend stack.
- **Angular frontend:** also listed in the syllabus. React was chosen because it is faster to work with and has a larger ecosystem (shadcn/ui, TanStack Query).
- **MySQL:** equally valid. PostgreSQL has better free hosting (Neon) and works well with Testcontainers.

## Consequences
- The project demonstrates the exact skills the role asks for, so the review can focus on them.
- Two apps and two runtimes mean more setup (two builds, CORS/proxy, two deploy targets) than a single full-stack framework.
- A clean REST API makes the frontend replaceable and the API testable on its own (Swagger, MockMvc).
