package com.am.marketdata.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [WHY THIS WAS ADDED]:
 * OkHttp interceptor that reads the raw Flux query body sent by InfluxDBClient,
 * extracts SAFE metadata (measurement name, filter field names, sanitized query
 * structure), and tags the current active Micrometer Tracing span.
 *
 * This allows you to see in Tempo:
 *   - db.influxdb.measurement  → which measurement was queried (e.g. "equity")
 *   - db.influxdb.filter_fields → which fields were filtered on (e.g. "symbol,isin")
 *   - db.influxdb.sanitized_query → the query structure with raw values replaced by "?"
 *
 * [SECURITY]:
 * Raw field values (e.g. "TCS", "INE009A01021", "NSE") are NEVER written to Tempo.
 * Only field names and structural patterns are captured.
 *
 * [SCOPE]:
 * Only activates for POST /api/v2/query and /api/v2/write InfluxDB endpoints.
 */
public class InfluxDbQuerySanitizerInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(InfluxDbQuerySanitizerInterceptor.class);

    // Matches: r._measurement == "equity"  → captures "equity"
    private static final Pattern MEASUREMENT_PATTERN =
            Pattern.compile("r\\._measurement\\s*==\\s*\"([^\"]+)\"");

    // Matches: r.symbol == "TCS"  → captures field name "symbol"
    // Skips internal InfluxDB fields that start with _ (e.g. _field, _value, _measurement)
    private static final Pattern FILTER_FIELD_PATTERN =
            Pattern.compile("r\\.([a-zA-Z][a-zA-Z0-9_]*)\\s*==\\s*\"[^\"]*\"");

    // Replaces raw values with ? for safe span tagging
    // e.g. r.symbol == "TCS"  →  r.symbol == "?"
    private static final Pattern SANITIZE_VALUES_PATTERN =
            Pattern.compile("(r\\.[a-zA-Z_][a-zA-Z0-9_]*\\s*==\\s*)\"[^\"]*\"");

    private final Tracer tracer;

    public InfluxDbQuerySanitizerInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String path = request.url().encodedPath();

        // Only process InfluxDB query and write API calls
        if (path.contains("/api/v2/query") || path.contains("/api/v2/write")) {
            String operation = path.contains("/api/v2/write") ? "write" : "query";
            
            // Create a dedicated InfluxDB child client span
            Span childSpan = tracer.spanBuilder()
                    .name("influxdb." + operation)
                    .kind(Span.Kind.CLIENT)
                    .start();
            
            childSpan.tag("db.system", "influxdb");
            childSpan.tag("peer.service", "influxdb");
            childSpan.tag("net.peer.name", request.url().host());
            childSpan.tag("net.peer.port", String.valueOf(request.url().port()));
            
            try (Tracer.SpanInScope ws = tracer.withSpan(childSpan)) {
                // Extract metadata and tag the child span
                tagSpan(childSpan, request, path);
                
                Response response = chain.proceed(request);
                
                childSpan.tag("http.status_code", String.valueOf(response.code()));
                if (!response.isSuccessful()) {
                    childSpan.tag("error", "true");
                    childSpan.tag("db.error", "HTTP " + response.code());
                }
                return response;
            } catch (Exception e) {
                childSpan.error(e);
                childSpan.tag("error", "true");
                throw e;
            } finally {
                childSpan.end();
            }
        }

        return chain.proceed(request);
    }

    private void tagSpan(Span span, Request request, String path) throws IOException {
        String body = readBody(request);
        if (body == null || body.isBlank()) {
            return;
        }

        // For /api/v2/write — it is a line protocol write, not a Flux query
        if (path.contains("/api/v2/write")) {
            String bucket = extractQueryParam(request, "bucket");
            span.tag("db.influxdb.operation", "write");
            if (!bucket.isEmpty()) {
                span.tag("db.influxdb.bucket", bucket);
            }
            return;
        }

        // For /api/v2/query — parse the Flux script body
        String measurement = extractFirst(body, MEASUREMENT_PATTERN);
        List<String> filterFields = extractFilterFields(body);
        String sanitized = SANITIZE_VALUES_PATTERN.matcher(body)
                .replaceAll("$1\"?\"")
                .replaceAll("\\s+", " ")
                .trim();

        // Tag the InfluxDB client span
        span.tag("db.influxdb.operation", "query");
        if (!measurement.isEmpty()) {
            span.tag("db.influxdb.measurement", measurement);
        }
        if (!filterFields.isEmpty()) {
            span.tag("db.influxdb.filter_fields", String.join(", ", filterFields));
        }
        if (!sanitized.isEmpty()) {
            // Limit to 500 chars to avoid bloating the span storage
            span.tag("db.influxdb.sanitized_query",
                    sanitized.length() > 500 ? sanitized.substring(0, 500) + "..." : sanitized);
        }
    }


    private String readBody(Request request) throws IOException {
        RequestBody body = request.body();
        if (body == null) return "";
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readUtf8();
    }

    private String extractFirst(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private List<String> extractFilterFields(String text) {
        List<String> fields = new ArrayList<>();
        Matcher m = FILTER_FIELD_PATTERN.matcher(text);
        while (m.find()) {
            String field = m.group(1);
            // Skip internal InfluxDB system column names (_measurement, _field, _value, _time)
            if (!field.startsWith("_") && !fields.contains(field)) {
                fields.add(field);
            }
        }
        return fields;
    }

    private String extractQueryParam(Request request, String paramName) {
        String value = request.url().queryParameter(paramName);
        return value != null ? value : "";
    }
}
