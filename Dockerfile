# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Add build arguments
ARG MAVEN_OPTS="-DskipTests"
ARG APP_PORT=8080

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Run Maven build
RUN mvn clean package ${MAVEN_OPTS}

# Run stage
FROM eclipse-temurin:21-jre-jammy

# Add labels
LABEL maintainer="Kevin Lee <cameljava@gmail.com>"
LABEL version="1.0"
LABEL description="Starling RoundUp Application"

# Add build arguments
ARG APP_PORT=8080

# Create a non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV APP_PORT=${APP_PORT}

# Expose the application port
EXPOSE ${APP_PORT}

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${APP_PORT}/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 