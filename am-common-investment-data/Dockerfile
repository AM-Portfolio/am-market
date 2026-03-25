# Stage 1: Build with Maven base image
ARG BASE_REGISTRY=""
FROM ${BASE_REGISTRY}am-java-maven-base:latest AS build

# Build arguments for GitHub authentication
ARG GITHUB_PACKAGES_USERNAME
ARG GITHUB_PACKAGES_TOKEN

WORKDIR /build

# Configure Maven settings for GitHub Packages
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml && \
    echo '<settings>' >> /root/.m2/settings.xml && \
    echo '  <servers>' >> /root/.m2/settings.xml && \
    echo '    <server>' >> /root/.m2/settings.xml && \
    echo '      <id>github-investment</id>' >> /root/.m2/settings.xml && \
    echo "      <username>${GITHUB_PACKAGES_USERNAME}</username>" >> /root/.m2/settings.xml && \
    echo "      <password>${GITHUB_PACKAGES_TOKEN}</password>" >> /root/.m2/settings.xml && \
    echo '    </server>' >> /root/.m2/settings.xml && \
    echo '    <server>' >> /root/.m2/settings.xml && \
    echo '      <id>github-am-common</id>' >> /root/.m2/settings.xml && \
    echo "      <username>${GITHUB_PACKAGES_USERNAME}</username>" >> /root/.m2/settings.xml && \
    echo "      <password>${GITHUB_PACKAGES_TOKEN}</password>" >> /root/.m2/settings.xml && \
    echo '    </server>' >> /root/.m2/settings.xml && \
    echo '  </servers>' >> /root/.m2/settings.xml && \
    echo '</settings>' >> /root/.m2/settings.xml

# Copy parent pom first
COPY pom.xml .

# Copy all module poms
COPY am-common-investment-model/pom.xml am-common-investment-model/
COPY am-common-investment-persistence/pom.xml am-common-investment-persistence/
COPY am-common-investment-service/pom.xml am-common-investment-service/
COPY am-common-investment-app/pom.xml am-common-investment-app/

# Copy source code
COPY am-common-investment-model/ am-common-investment-model/
COPY am-common-investment-persistence/ am-common-investment-persistence/
COPY am-common-investment-service/ am-common-investment-service/
COPY am-common-investment-app/ am-common-investment-app/

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime with JRE
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /build/am-common-investment-app/target/*.jar app.jar

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
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
