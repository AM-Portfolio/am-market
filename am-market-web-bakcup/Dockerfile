# Multi-Stage Build for Flutter Web
FROM am-flutter-base:latest AS build

WORKDIR /app

# Copy shared libraries and SDKs with correct directory structure
# Context is AM-Portfolio/
COPY am-ui-components/am-ui/am_common_ui /app/am_common_ui
COPY am-market/am_market_sdk_flutter /app/am-market/am_market_sdk_flutter
COPY am-market/am-market-web /app/am-market/am-market-web

# Setup market web directory
WORKDIR /app/am-market/am-market-web

# Clean and get dependencies
# Using --verbose to see where it hangs or fails
RUN rm -f pubspec.lock && flutter pub get -v

# Build web application
RUN flutter build web --release --base-href /market-ui/

# Serve with Nginx
FROM nginx:alpine
COPY --from=build /app/am-market/am-market-web/build/web /usr/share/nginx/html/market-ui
COPY am-market/am-market-web/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
