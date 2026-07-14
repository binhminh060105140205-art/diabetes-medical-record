FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src src
RUN mvn -B clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 spring
COPY --from=build /app/target/diabetes-medical-record.war app.war
RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring
EXPOSE 10000
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.war"]
