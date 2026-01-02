package com.am.common.investment.service.impl;

import com.am.common.investment.model.equity.*;
import com.am.common.investment.persistence.influx.measurement.MarketIndexIndicesMeasurement;
import com.am.common.investment.persistence.repository.measurement.MarketIndexIndicesRepository;
import com.am.common.investment.service.MarketIndexIndicesService;
import com.am.common.investment.service.mapper.MarketIndexIndicesMapper;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketIndexIndicesServiceImpl implements MarketIndexIndicesService {
    private static final Logger logger = LoggerFactory.getLogger(MarketIndexIndicesServiceImpl.class);

    private final MarketIndexIndicesRepository marketIndexRepository;
    private final MarketIndexIndicesMapper mapper;

    @Override
    public void save(MarketIndexIndices indices) {
        logger.debug("Converting model to measurement: key={}, index={}, symbol={}", 
            indices.getKey(), indices.getIndex(), indices.getIndexSymbol());
        try {
            MarketIndexIndicesMeasurement measurement = mapper.convertToMeasurement(indices);
            if (measurement == null) {
                logger.error("Failed to convert indices to measurement, skipping save");
                return;
            }
            
            logger.debug("Converted measurement: measurement={}, key={}, index={}, symbol={}, time={}", 
                measurement, measurement.getKey(), measurement.getIndex(), measurement.getIndexSymbol(), measurement.getTime());
            
            logger.debug("Writing point to InfluxDB: measurement=market_index, key={}, index={}, symbol={}", 
                measurement.getKey(), measurement.getIndex(), measurement.getIndexSymbol());
            
            marketIndexRepository.save(measurement);
            logger.debug("Successfully wrote point to InfluxDB");
            
        } catch(Exception e) {
            logger.error("Error saving measurement", e);
            throw new RuntimeException("Failed to save market index measurement", e);
        }
    }

    @Override
    public List<MarketIndexIndices> getByKey(String key) {
        logger.debug("Finding all indices by key: {}", key);
        return marketIndexRepository.findByKey(key).stream()
            .map(mapper::convertToModel)
            .collect(Collectors.toList());
    }
}
