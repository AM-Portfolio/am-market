# Multi-stage build for Market Data Service
# Stage 1: Build with Maven base image
ARG BASE_REGISTRY=""
FROM ${BASE_REGISTRY}am-java-maven-base:latest AS build

# Build arguments for GitHub authentication
ARG GITHUB_PACKAGES_USERNAME
ARG GITHUB_PACKAGES_TOKEN

WORKDIR /build

# Build common investment data first (from root context)
# This fulfills the requirement: "first check in local if not then github"
COPY am-common-investment-data/ am-common-investment-data/
RUN cd am-common-investment-data && \
  GITHUB_PACKAGES_USERNAME=${GITHUB_PACKAGES_USERNAME} GITHUB_PACKAGES_TOKEN=${GITHUB_PACKAGES_TOKEN} mvn clean install -DskipTests -B

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
RUN GITHUB_PACKAGES_USERNAME=${GITHUB_PACKAGES_USERNAME} GITHUB_PACKAGES_TOKEN=${GITHUB_PACKAGES_TOKEN} mvn clean package -DskipTests -B

# Stage 2: Runtime with JRE
# Absolute final lean stabilization rollout: 2026-04-11T15:45:00Z
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Install curl, Google Chrome for Selenium scraper, and set timezone (cached layer).
# Use resilient DNS resolvers and Azure Ubuntu mirror for reliable builds on CI runners.
RUN set -eux; \
  export DEBIAN_FRONTEND=noninteractive; \
  printf "nameserver 8.8.8.8\nnameserver 1.1.1.1\n" > /etc/resolv.conf; \
  if [ -f /etc/apt/sources.list ]; then \
    sed -i 's/archive.ubuntu.com/azure.archive.ubuntu.com/g' /etc/apt/sources.list; \
    sed -i 's/security.ubuntu.com/azure.archive.ubuntu.com/g' /etc/apt/sources.list; \
  fi; \
  apt-get update; \
  apt-get install -y --no-install-recommends curl wget gnupg; \
  wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -; \
  echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google.list; \
  apt-get update; \
  apt-get install -y --no-install-recommends google-chrome-stable; \
  rm -rf /var/lib/apt/lists/*; \
  ln -snf /usr/share/zoneinfo/Asia/Kolkata /etc/localtime; \
  echo "Asia/Kolkata" > /etc/timezone

# Copy data directory for instrument seeding
COPY am-market-data/data/ data/

# Copy the built JAR from build stage
COPY --from=build /build/am-market-data/market-data-app/target/*.jar app.jar

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV TZ=Asia/Kolkata

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
