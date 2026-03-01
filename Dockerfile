# =========================
# Stage 1 - Build
# =========================
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# =========================
# Stage 2 - Run
# =========================
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
# ---- Build arguments from CI ----
ARG GIT_COMMIT=unknown
ARG BUILD_TIME=unknown

# ---- Inject into container ----
ENV APP_GIT_COMMIT=$GIT_COMMIT
ENV APP_BUILD_TIME=$BUILD_TIME

# Optional: OCI labels (nice for Docker Hub & inspect)
LABEL org.opencontainers.image.revision=$GIT_COMMIT
LABEL org.opencontainers.image.created=$BUILD_TIME
LABEL org.opencontainers.image.source="https://github.com/devdao2002/enterprise-rag-assistant"

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]