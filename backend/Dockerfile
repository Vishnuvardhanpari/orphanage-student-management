# Multi-stage build for Cloud Run deployment.
# Build context: backend/
# Usage: docker build -t oms-backend .

FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S oms && adduser -S oms -G oms

WORKDIR /app

COPY --from=build /app/target/oms-*.jar app.jar

RUN chown -R oms:oms /app

USER oms

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
