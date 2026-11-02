# =============================================================================
# Stage 1 — build
# Full Maven + JDK 21 image used only to compile and package the application.
# This stage is discarded after the JAR is extracted; it never reaches prod.
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Copy dependency descriptor first so Docker can cache the dependency-download
# layer independently of source-code changes.
COPY pom.xml .

# Download all dependencies (offline-friendly cache layer).
# -B  : batch/non-interactive  -q : quiet  -e : show errors
RUN mvn -B -q -e dependency:go-offline

# Copy the full source tree and run the Maven package phase.
# Tests are skipped here; run them in CI before building the image.
# The spring-boot-maven-plugin repackages the output into a single
# executable fat JAR under target/*.jar.
COPY src ./src
RUN mvn -B -q -e package -DskipTests

# Locate the repackaged fat JAR (excludes the *.original plain JAR that the
# plugin also writes) and copy it to a fixed, version-agnostic name so the
# next stage does not need to know the project version string.
RUN cp target/*.jar target/app.jar

# =============================================================================
# Stage 2 — runtime
# Minimal JRE 21 Alpine image — no compiler, no Maven, no source code.
# Results in a significantly smaller final image (~200 MB vs ~500 MB).
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Run as a non-root user for better container security.
RUN addgroup -S civicpulse && adduser -S civicpulse -G civicpulse
USER civicpulse

WORKDIR /app

# Pull only the fixed-name JAR from the build stage.
COPY --from=build /workspace/target/app.jar app.jar

# Inform Docker (and orchestrators) which port the application listens on.
EXPOSE 8080

# Launch the Spring Boot fat JAR.
# JVM flags:
#   -XX:+UseContainerSupport          honour cgroup CPU/memory limits
#   -XX:MaxRAMPercentage=75.0         cap heap at 75 % of the container RAM
#   -Djava.security.egd=…urandom      faster SecureRandom startup (avoids
#                                     /dev/random entropy starvation in containers)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

