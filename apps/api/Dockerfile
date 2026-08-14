FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY apps/api ./apps/api
RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && for attempt in 1 2 3; do \
      ./gradlew :apps:api:bootJar --no-daemon && break; \
      test "$attempt" -lt 3 || exit 1; \
      sleep "$((attempt * 5))"; \
    done

FROM eclipse-temurin:25-jre-jammy AS runtime
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid 10001 --home-dir /app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/apps/api/build/libs/*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
