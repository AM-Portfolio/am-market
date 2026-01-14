


# Multi-stage build for Market Data Service
# Stage 1: Build with Maven base image
FROM am-java-maven-base:latest AS build

# Build arguments for GitHub authentication
ARG GITHUB_PACKAGES_USERNAME
ARG GITHUB_PACKAGES_TOKEN

# Set environment variables for Maven to pick up from settings.xml
ENV GITHUB_PACKAGES_USERNAME=${GITHUB_PACKAGES_USERNAME}
ENV GITHUB_PACKAGES_TOKEN=${GITHUB_PACKAGES_TOKEN}

WORKDIR /build

# Build common investment data first (from root context)
# This fulfills the requirement: "first check in local if not then github"
COPY am-common-investment-data/ am-common-investment-data/
RUN cd am-common-investment-data && \
  mvn clean install -DskipTests -B

# Now build Market Data modules
WORKDIR /build/am-market-data

# Copy settings.xml to Maven config directory
RUN mkdir -p /root/.m2
COPY am-market-data/settings.xml /root/.m2/settings.xml

# Copy parent pom first
COPY am-market-data/pom.xml .

# Copy all module poms
COPY am-market-data/market-data-common/pom.xml market-data-common/
COPY am-market-data/market-data-provider/pom.xml market-data-provider/
COPY am-market-data/market-data-kafka/pom.xml market-data-kafka/
COPY am-market-data/market-data-service/pom.xml market-data-service/
COPY am-market-data/market-data-scraper/pom.xml market-data-scraper/
COPY am-market-data/market-data-scheduler/pom.xml market-data-scheduler/
COPY am-market-data/market-data-api/pom.xml market-data-api/
COPY am-market-data/market-data-app/pom.xml market-data-app/
COPY am-market-data/market-data-redis/pom.xml market-data-redis/
COPY am-market-data/market-data-internal/pom.xml market-data-internal/
COPY am-market-data/market-data-analysis/pom.xml market-data-analysis/
COPY am-market-data/market-data-watchlist/pom.xml market-data-watchlist/

# Copy source code
COPY am-market-data/market-data-common/ market-data-common/
COPY am-market-data/market-data-provider/ market-data-provider/
COPY am-market-data/market-data-kafka/ market-data-kafka/
COPY am-market-data/market-data-service/ market-data-service/
COPY am-market-data/market-data-scraper/ market-data-scraper/
COPY am-market-data/market-data-scheduler/ market-data-scheduler/
COPY am-market-data/market-data-api/ market-data-api/
COPY am-market-data/market-data-app/ market-data-app/
COPY am-market-data/market-data-redis/ market-data-redis/
COPY am-market-data/market-data-internal/ market-data-internal/
COPY am-market-data/market-data-analysis/ market-data-analysis/
COPY am-market-data/market-data-watchlist/ market-data-watchlist/

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime with JRE
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /build/am-market-data/market-data-app/target/*.jar app.jar

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
EXPOSE 8092

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8092/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
