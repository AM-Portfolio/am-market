package com.am.common.investment.persistence.repository.measurement.impl;

import com.am.common.investment.persistence.influx.measurement.MarketIndexIndicesMeasurement;
import com.am.common.investment.persistence.repository.measurement.MarketIndexIndicesRepository;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MarketIndexIndicesRepositoryImpl implements MarketIndexIndicesRepository {
    private static final Logger logger = LoggerFactory.getLogger(MarketIndexIndicesRepositoryImpl.class);
    private static final String MEASUREMENT_NAME = "market_index";

    @Value("${spring.influx.bucket}")
    private String bucket;

    @Value("${spring.influx.org}")
    private String org;

    private final InfluxDBClient influxDBClient;

    @Override
    public void save(MarketIndexIndicesMeasurement measurement) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi(WriteOptions.builder().batchSize(1).build())) {
            logger.debug("Saving measurement to InfluxDB: measurement=market_index, index={}, key={}, time={}", 
                measurement.getIndex(), measurement.getKey(), measurement.getTime());

            Point point = Point.measurement(MEASUREMENT_NAME)
                .addTag("key", measurement.getKey())
                .addTag("index", measurement.getIndex())
                .addTag("indexSymbol", measurement.getIndexSymbol())
                .addField("market_data_open", measurement.getMarketDataOpen())
                .addField("market_data_previous_close", measurement.getMarketDataPreviousClose())
                .addField("market_data_high", measurement.getMarketDataHigh())
                .addField("market_data_low", measurement.getMarketDataLow())
                .addField("market_data_close", measurement.getMarketDataClose())
                .addField("market_data_percentage_change", measurement.getMarketDataPercentageChange())
                .time(measurement.getTime(), WritePrecision.NS);
            
            logger.debug("Writing measurement to InfluxDB: {}", point.toString());
            writeApi.writePoint(bucket, org, point);
            writeApi.flush();
            logger.debug("Successfully wrote measurement to InfluxDB");
        } catch (Exception e) {
            logger.error("Failed to write measurement to InfluxDB: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save market index measurement", e);
        }
    }

    @Override
    public List<MarketIndexIndicesMeasurement> findByKey(String key) {
        String escapedKey = key.replace(" ", "\\ ");
        String query = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: -30d) " +
            "|> filter(fn: (r) => r._measurement == \"%s\") " +
            "|> filter(fn: (r) => r.key == \"%s\") " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") " +
            "|> last()",
            bucket, MEASUREMENT_NAME, escapedKey
        );

        logger.debug("Executing findByKey query: measurement=market_index, key={}", key);
        logger.debug("Query: {}", query);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<MarketIndexIndicesMeasurement> results = queryApi.query(query, MarketIndexIndicesMeasurement.class);
        logger.debug("Found {} results for key={}", results.size(), key);

        if (!results.isEmpty()) {
            // Set the tags since they are not included in the pivot result
            results.forEach(m -> {
                m.setKey(key);
                m.setIndex(m.getIndex());
                m.setIndexSymbol(m.getIndexSymbol());
            });
        }

        return results;
    }
}
