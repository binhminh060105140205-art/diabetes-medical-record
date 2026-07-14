FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src src
RUN for attempt in 1 2 3 4 5; do \
      mvn -B -DskipTests \
        -Dmaven.wagon.http.retryHandler.count=5 \
        clean package && exit 0; \
      echo "Maven Central download failed (attempt ${attempt}/5); retrying..."; \
      sleep $((attempt * 10)); \
    done; \
    exit 1

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 spring
COPY --from=build /app/target/diabetes-medical-record.war app.war
RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring
EXPOSE 10000
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.war"]
