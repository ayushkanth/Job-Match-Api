# Multi-stage build for Job Match API
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and POM first to leverage Docker layer caching
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build production jar (skipping tests for fast container builds)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Runtime image using minimal JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root security user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=build /app/target/job-match-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
