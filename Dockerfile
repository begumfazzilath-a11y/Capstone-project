# ---------------------------------------------------------------------------
# FazziMart - production image (Docker / Render.com / any Docker host)
#
#   Stage 1 : build the Spring Boot JAR with Maven + JDK 17
#   Stage 2 : run it on a slim JRE 17. The JAR already contains the frontend
#             (bundled at classpath:/static/ by pom.xml), so one image serves
#             the whole store.
#
# DB credentials come from env vars DB_URL / DB_USER / DB_PASSWORD; the port
# is taken from $PORT (Render injects it) and defaults to 9090 locally.
# ---------------------------------------------------------------------------

# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Crawl dependencies first (cached unless pom.xml changes)
COPY backend/pom.xml backend/pom.xml
WORKDIR /build/backend
RUN mvn -q -B dependency:go-offline || true

# Sources + frontend (required by the prepare-package static bundle step)
COPY backend/src ./src
COPY frontend/ ../frontend/
RUN mvn -q -B clean package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/backend/target/fazzi-mart-backend-1.0.0.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 9090
ENTRYPOINT ["/app/docker-entrypoint.sh"]