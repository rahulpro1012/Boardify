# Stage 1: Build the JAR
FROM maven:3.9-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-Xmx256m", "-Xss512k", "-XX:+UseContainerSupport", "-jar", "app.jar"]
