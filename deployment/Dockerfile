# Build from the repository root: docker build -f deployment/Dockerfile -t reto-gestion-franquicias .
#
# Stage 1 compiles the jar with the full JDK. Stage 2 only carries a JRE and the jar, runs as a non-root user
# and holds no source code, build tools or secrets: ECS injects the DB_* values from AWS Secrets Manager.

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :app-service:bootJar -x test --no-daemon --console=plain

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/applications/app-service/build/libs/*.jar /app/app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
