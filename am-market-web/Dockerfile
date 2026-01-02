# Multi-Stage Build for Flutter Web
FROM am-flutter-base:latest AS build

WORKDIR /app

# Copy shared library
COPY am_common_ui /app/am_common_ui
COPY am-market/am_market_sdk_flutter /app/am-market/am_market_sdk_flutter

# Setup market web directory
WORKDIR /app/am-market/am-market-web

# Copy pubspec files
COPY am-market/am-market-web/pubspec.yaml am-market/am-market-web/pubspec.lock ./

# Get dependencies
RUN flutter pub get

# Copy source code
COPY am-market/am-market-web/ .

# Build web application
RUN flutter build web --release --base-href /market-ui/

# Serve with Nginx
FROM nginx:alpine
COPY --from=build /app/am-market/am-market-web/build/web /usr/share/nginx/html/market-ui
COPY am-market/am-market-web/nginx.conf /etc/nginx/conf.d/default.conf
# Fix permissions so nginx can read all files
RUN chmod -R 755 /usr/share/nginx/html/market-ui
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
