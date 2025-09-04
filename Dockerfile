# Build stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies
RUN gradle dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the application (skip tests)
RUN gradle build -x test --no-daemon

# Production stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user
RUN addgroup --system --gid 1001 spring
RUN adduser --system --uid 1001 spring

# Copy built jar
COPY --from=builder /app/build/libs/bms-control-system-back-2-0.0.1-SNAPSHOT.jar app.jar

# Set ownership
RUN chown -R spring:spring /app
USER spring

# Expose port
EXPOSE 8080

# Start the application
CMD ["java", "-jar", "app.jar"]
