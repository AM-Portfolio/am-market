package com.am.marketdata.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpObservationInterceptor;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Custom InfluxDB Configuration with Micrometer Distributed Tracing.
 *
 * Two interceptors are chained into the OkHttp client that InfluxDBClient uses:
 *
 * 1. InfluxDbQuerySanitizerInterceptor (runs FIRST):
 *    Reads the raw Flux query body, extracts the measurement name and filter
 *    field names (not values), and tags the current active Tempo span with:
 *      - db.influxdb.measurement     → e.g. "equity"
 *      - db.influxdb.filter_fields   → e.g. "symbol, isin"
 *      - db.influxdb.sanitized_query → query with raw values replaced by "?"
 *
 * 2. OkHttpObservationInterceptor (runs SECOND):
 *    Creates the actual Tempo span for the HTTP request with standard HTTP
 *    tags (method, url, status code).
 *
 * Named 'tracedInfluxDBConfig' to avoid naming conflicts with the library's
 * config in com.am.common.
 */
@Configuration("tracedInfluxDBConfig")
public class TracedInfluxDBConfig {

    @Value("${spring.influx.url}")
    private String url;

    @Value("${spring.influx.token}")
    private String token;

    @Value("${spring.influx.org}")
    private String org;

    @Value("${spring.influx.bucket}")
    private String bucket;

    /**
     * Overrides the default influxDBClient bean with our traced client.
     * Marked as @Primary to ensure it is preferred for injection.
     *
     * ObjectProvider is used for both ObservationRegistry and Tracer to avoid
     * eager startup dependency issues (same pattern used across this library).
     */
    @Bean
    @Primary
    public InfluxDBClient influxDBClient(
            ObjectProvider<ObservationRegistry> observationRegistryProvider,
            ObjectProvider<Tracer> tracerProvider) {

        okhttp3.OkHttpClient.Builder okHttpClientBuilder = new okhttp3.OkHttpClient.Builder();

        Tracer tracer = tracerProvider.getIfAvailable();
        ObservationRegistry registry = observationRegistryProvider.getIfAvailable();

        // Interceptor 1: Query sanitizer — extracts safe metadata and tags the Tempo span
        // Must run BEFORE OkHttpObservationInterceptor so the span already exists when we tag it
        if (tracer != null) {
            okHttpClientBuilder.addInterceptor(new InfluxDbQuerySanitizerInterceptor(tracer));
        }

        // Interceptor 2: Creates the Tempo HTTP span (method, url, status)
        if (registry != null) {
            okHttpClientBuilder.addInterceptor(
                OkHttpObservationInterceptor.builder(registry, "influxdb.requests").build()
            );
        }

        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(url)
                .authenticateToken(token.toCharArray())
                .org(org)
                .bucket(bucket)
                .okHttpClient(okHttpClientBuilder)
                .build();

        return InfluxDBClientFactory.create(options);
    }

    public String getBucket() {
        return bucket;
    }

    public String getOrg() {
        return org;
    }
}
