# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 for attempt in 1 2 3; do \
      mvn -B -DskipTests \
        -Dmaven.wagon.http.retryHandler.count=5 \
        package && exit 0; \
      echo "Maven build failed (attempt ${attempt}/3); retrying..."; \
      sleep $((attempt * 5)); \
    done; \
    exit 1

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 spring
COPY --from=build /app/target/diabetes-medical-record.war app.war
RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring
EXPOSE 10000
ENTRYPOINT ["java","-XX:+UseSerialGC","-XX:InitialRAMPercentage=20.0","-XX:MaxRAMPercentage=60.0","-Xss512k","-jar","/app/app.war"]
