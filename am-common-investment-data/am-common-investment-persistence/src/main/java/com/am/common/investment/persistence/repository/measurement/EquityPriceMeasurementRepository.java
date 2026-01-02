package com.am.common.investment.persistence.repository.measurement;

import com.am.common.investment.persistence.influx.measurement.EquityPriceMeasurement;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EquityPriceMeasurementRepository extends Repository<EquityPriceMeasurement, String> {

    void saveAll(List<EquityPriceMeasurement> measurements);
    // Find by symbol
     Optional<EquityPriceMeasurement> findLatestBySymbol(String symbol);
     List<EquityPriceMeasurement> findBySymbol(String symbol);
    List<EquityPriceMeasurement> findBySymbolAndTimeBetween(String symbol, Instant startTime, Instant endTime);
    
    // // Find by ISIN
     Optional<EquityPriceMeasurement> findLatestByIsin(String isin);
     List<EquityPriceMeasurement> findByIsin(String isin);
     List<EquityPriceMeasurement> findByIsinAndTimeBetween(String isin, Instant startTime, Instant endTime);
    
     List<EquityPriceMeasurement> findByKeyAndTimeBetween(String key, Instant startTime, Instant endTime);
    
    // Find by exchange and currency
     List<EquityPriceMeasurement> findByExchange(String exchange);
     
    // Find by multiple trading symbols
     List<EquityPriceMeasurement> findByTradingSymbolIn(List<String> tradingSymbols);
     
    // Find by multiple ISINs
     List<EquityPriceMeasurement> findByIsinIn(List<String> isins);
    
     // Find latest by symbol or ISIN
     default Optional<EquityPriceMeasurement> findLatestByKey(String key) {
         Optional<EquityPriceMeasurement> bySymbol = findLatestBySymbol(key);
         if (bySymbol.isPresent()) {
             return bySymbol;
         }
        return findLatestByIsin(key);
     }
	
	 // Basic CRUD operations
     void save(EquityPriceMeasurement measurement);

}
