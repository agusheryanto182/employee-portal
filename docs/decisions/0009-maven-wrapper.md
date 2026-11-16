# 0009. Maven with the Maven Wrapper

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
The backend needs a build tool. The local machine has a JDK but no global Maven. The build must give the same result on the developer's laptop, a reviewer's laptop, GitHub Actions and the Docker build on Render.

## Decision
Use **Maven** through the **Maven Wrapper** (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`), committed to the repository with a pinned Maven version.
- CI, the Dockerfile and the README always call `./mvnw`, never `mvn`.
- `.gitattributes` keeps `mvnw` with LF line endings and `mvnw.cmd` with CRLF so the scripts work after cloning on any OS.
- `mvnw` keeps its executable bit in Git.

## Alternatives considered
- **Gradle (with its wrapper):** faster incremental builds and a more flexible DSL, but the speed benefit is negligible for a project this size, and `pom.xml` is easier for reviewers to read.
- **Global Maven install only:** every machine needs the right Maven version installed; builds can differ between machines.

## Consequences
- Anyone with a JDK can build and run the backend: `./mvnw spring-boot:run`.
- Upgrading Maven is a one-line change tracked in Git.
- A few extra files in the repository, and the first run downloads Maven (cached afterwards in `~/.m2/wrapper`).
