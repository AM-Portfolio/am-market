# Runtime stage using pre-built JAR
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the pre-built JAR file from the correct location
COPY market-data-app/target/*.jar app.jar

# Install curl for healthcheck
RUN apt-get update && \
  apt-get install -y curl && \
  rm -rf /var/lib/apt/lists/* && \
  # Set timezone
  ln -sf /usr/share/zoneinfo/Asia/Kolkata /etc/localtime

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV TZ=Asia/Kolkata

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8092/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
