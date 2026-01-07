# Multi-stage Flutter web build
# Use official Flutter image
FROM ghcr.io/cirruslabs/flutter:stable AS build-env

# Create app directory
WORKDIR /app

# Copy dependencies from AM-Repos
COPY AM-Repos/ui/am_modern_ui/am_design_system ./ui/am_modern_ui/am_design_system
COPY AM-Repos/ui/am_modern_ui/am_common ./ui/am_modern_ui/am_common
COPY AM-Repos/ui/am_modern_ui/am_auth_ui ./ui/am_modern_ui/am_auth_ui

# Copy external SDK dependency
COPY AM-Repos/backend/am-market/am_market_sdk_flutter ./backend/am-market/am_market_sdk_flutter

# Copy complete module (includes lib/ and live/)
COPY AM-Repos/ui/am_modern_ui/am_market_ui ./ui/am_modern_ui/am_market_ui
WORKDIR /app/ui/am_modern_ui/am_market_ui/live

# Get dependencies
RUN flutter pub get

# Build web app
RUN flutter build web --release

# Runtime stage
FROM nginx:alpine

# Copy built files
COPY --from=build-env /app/am_market_ui/live/build/web /usr/share/nginx/html

# Expose port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
