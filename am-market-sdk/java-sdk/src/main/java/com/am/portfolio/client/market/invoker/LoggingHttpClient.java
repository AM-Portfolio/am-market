package com.am.portfolio.client.market.invoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

public class LoggingHttpClient extends HttpClient {

    private static final Logger logger = LoggerFactory.getLogger(LoggingHttpClient.class);
    private final HttpClient delegate;

    public LoggingHttpClient(HttpClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return delegate.cookieHandler();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public Redirect followRedirects() {
        return delegate.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return delegate.proxy();
    }

    @Override
    public SSLContext sslContext() {
        return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return delegate.sslParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return delegate.authenticator();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public Optional<Executor> executor() {
        return delegate.executor();
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        logRequest(request);

        HttpResponse<T> response = delegate.send(request, responseBodyHandler);

        long duration = System.currentTimeMillis() - start;
        return logResponse(response, duration);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler) {
        return delegate.sendAsync(request, responseBodyHandler);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        return delegate.sendAsync(request, responseBodyHandler, pushPromiseHandler);
    }

    // Helper to log request as cURL
    private void logRequest(HttpRequest request) {
        // Request Body capture for summary and full trace
        String body = request.bodyPublisher().map(this::getRequestBody).orElse("");
        String bodySummary = (body.length() > 50) ? body.substring(0, 47) + "..." : body;

        // Minimal INFO log
        logger.info("API Request: {} {} {}", request.method(), request.uri(),
                bodySummary.isEmpty() ? "" : "(Body: " + bodySummary + ")");

        if (logger.isDebugEnabled()) {
            StringBuilder curl = new StringBuilder("curl -v");
            curl.append(" -X ").append(request.method());

            request.headers().map().forEach(
                    (k, v) -> v.forEach(val -> curl.append(" -H '").append(k).append(": ").append(val).append("'")));

            if (!body.isEmpty() && !"[body]".equals(body)) {
                curl.append(" --data '").append(body).append("'");
            }

            curl.append(" '").append(request.uri()).append("'");
            logger.debug("API Request (cURL):\n{}", curl);
        }
    }

    private String getRequestBody(HttpRequest.BodyPublisher publisher) {
        try {
            // Attempt to read from common JDK-internal BodyPublishers using reflection
            Class<?> clazz = publisher.getClass();
            java.lang.reflect.Field field = null;

            // Checking for common field names in JDK internal BodyPublishers
            String[] possibleFields = { "content", "s", "bytes" };
            for (String fieldName : possibleFields) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                    if (field != null)
                        break;
                } catch (NoSuchFieldException e) {
                    // continue
                }
            }

            if (field != null) {
                field.setAccessible(true);
                Object content = field.get(publisher);
                if (content instanceof byte[]) {
                    return new String((byte[]) content, StandardCharsets.UTF_8);
                } else if (content instanceof String) {
                    return (String) content;
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private <T> HttpResponse<T> logResponse(HttpResponse<T> response, long duration) {
        // Minimal INFO log
        logger.info("API Response ({} ms): Status {}", duration, response.statusCode());

        // Body Logging and Buffering (at DEBUG level)
        T body = response.body();
        if (body instanceof InputStream) {
            byte[] bytes = null;
            try (InputStream is = (InputStream) body) {
                bytes = is.readAllBytes();
                if (logger.isDebugEnabled()) {
                    String bodyString = new String(bytes, StandardCharsets.UTF_8);
                    logger.debug("Response Body ({} bytes):\n{}", bytes.length, bodyString);
                }
            } catch (Exception e) {
                logger.error("Failed to read response body for logging", e);
            }

            if (bytes != null) {
                // Return wrapped response with new InputStream
                return new WrapperHttpResponse<>(response, (T) new ByteArrayInputStream(bytes));
            }
        } else if (body != null) {
            logger.debug("Response Body:\n{}", body);
        }

        return response;
    }

    // Wrapper for HttpResponse to override body()
    private static class WrapperHttpResponse<T> implements HttpResponse<T> {
        private final HttpResponse<T> delegate;
        private final T body;

        public WrapperHttpResponse(HttpResponse<T> delegate, T body) {
            this.delegate = delegate;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return delegate.statusCode();
        }

        @Override
        public HttpRequest request() {
            return delegate.request();
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return delegate.previousResponse();
        }

        @Override
        public HttpHeaders headers() {
            return delegate.headers();
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return delegate.sslSession();
        }

        @Override
        public URI uri() {
            return delegate.uri();
        }

        @Override
        public Version version() {
            return delegate.version();
        }
    }
}
