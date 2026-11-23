# ==============================================================================
# Multi-stage production build for Sambar & Spicy Spring Boot Backend
# ==============================================================================

# Stage 1: Build the JAR
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root system group and user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create directory for image uploads with write permissions
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# Production JVM optimizations for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
