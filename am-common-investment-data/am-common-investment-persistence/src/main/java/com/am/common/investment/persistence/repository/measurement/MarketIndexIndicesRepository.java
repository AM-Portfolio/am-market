package com.am.common.investment.persistence.repository.measurement;

import com.am.common.investment.persistence.influx.measurement.MarketIndexIndicesMeasurement;

import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MarketIndexIndicesRepository extends Repository<MarketIndexIndicesMeasurement, String> {
    
    // Basic CRUD operations
    void save(MarketIndexIndicesMeasurement measurement);
    List<MarketIndexIndicesMeasurement> findByKey(String key);
}
