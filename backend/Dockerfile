# syntax=docker/dockerfile:1

# Build: compile the Spring Boot jar with the Maven Wrapper (tests run in CI, not here).
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# Run: JRE only, as a non-root user.
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /app/app.jar app.jar
# Sized for small containers such as Render's free 512 MB: heap at 75% of the container limit,
# the serial collector (least overhead on one CPU), and exit on OOM so the platform restarts it.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
