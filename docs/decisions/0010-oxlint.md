# 0010. oxlint for frontend linting

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
The frontend needs a linter in CI. The current Vite React template ships with **oxlint** instead of ESLint.

| | ESLint | oxlint |
|---|---|---|
| Written in | JavaScript | Rust (Oxc project) |
| Speed | Slower on large codebases | Roughly 50–100× faster |
| Rules | Thousands, through plugins | Hundreds, ported from the most used ESLint rules |
| Type-aware TypeScript rules | Mature (typescript-eslint) | Limited |
| Config | `eslint.config.js` | Near zero-config |

## Decision
Keep **oxlint** with the template's configuration (`frontend/.oxlintrc.json`) and run it in CI. Part of the reason is to learn the tool.

## Alternatives considered
- **ESLint + typescript-eslint + react-hooks:** the industry standard and likely what the company uses, with mature type-aware rules such as `no-floating-promises`. Not chosen for now.
- **Both (oxlint first, ESLint for the remaining rules):** common in large codebases, but too much tooling for a project this size.

## Consequences
- Linting is near-instant and needs almost no configuration.
- Some type-aware checks (for example unawaited promises) are not covered; strict TypeScript settings and code review compensate.
- Switching to ESLint later is cheap: add `eslint.config.js`, change the `lint` script, and supersede this ADR.
