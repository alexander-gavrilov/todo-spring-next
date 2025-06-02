# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim AS builder

# Set the working directory in the container
WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies (optional, but can speed up subsequent builds if source changes more often than dependencies)
# RUN ./mvnw dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Package the application
RUN ./mvnw package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install curl for healthchecks
RUN apk --no-cache add curl

WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
