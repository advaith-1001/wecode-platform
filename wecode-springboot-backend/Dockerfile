# --- Stage 1: Build the application JAR ---
# Use a base image with Maven and JDK to build the application
FROM maven:3.8-openjdk-17 AS builder

WORKDIR /app

# Copy the pom.xml first to leverage Docker's layer caching
COPY pom.xml .

# Download all dependencies
RUN mvn dependency:go-offline

# Copy the rest of your source code
COPY src ./src

# Package the application, skipping the tests
RUN mvn package -DskipTests


# --- Stage 2: Create the final, lightweight image ---
# Use a minimal, secure base image with only the Java Runtime Environment
# CORRECTED IMAGE: Using Eclipse Temurin, a trusted producer of OpenJDK builds
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the executable JAR file from the 'builder' stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port your application runs on
EXPOSE 8080

# The command to run your application
ENTRYPOINT ["java", "-jar", "app.jar"]