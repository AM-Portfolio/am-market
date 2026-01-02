// package com.am.common.investment.persistence.repository.measurement;

// import com.am.common.investment.persistence.influx.measurement.EquityFundamentalMeasurement;
// import org.springframework.data.repository.Repository;

// import java.time.Instant;
// import java.util.List;
// import java.util.Optional;

// public interface EquityFundamentalMeasurementRepository extends Repository<EquityFundamentalMeasurement, String> {
    
//     // Basic CRUD operations
//     void save(EquityFundamentalMeasurement measurement);
//     void saveAll(Iterable<EquityFundamentalMeasurement> measurements);
    
//     // Find by symbol
//     Optional<EquityFundamentalMeasurement> findLatestBySymbol(String symbol);
//     List<EquityFundamentalMeasurement> findBySymbol(String symbol);
//     List<EquityFundamentalMeasurement> findBySymbolAndTimeBetween(String symbol, Instant startTime, Instant endTime);
    
//     // Find by ISIN
//     Optional<EquityFundamentalMeasurement> findLatestByIsin(String isin);
//     List<EquityFundamentalMeasurement> findByIsin(String isin);
//     List<EquityFundamentalMeasurement> findByIsinAndTimeBetween(String isin, Instant startTime, Instant endTime);
    
//     // Advanced queries
//     List<EquityFundamentalMeasurement> findByMarketCapGreaterThan(Double marketCap);
//     List<EquityFundamentalMeasurement> findByPeLessThan(Double pe);
//     List<EquityFundamentalMeasurement> findByDividendYieldGreaterThan(Double dividendYield);
    
//     // Aggregation queries
//     Double findAveragePeBySymbolAndTimeBetween(String symbol, Instant startTime, Instant endTime);
//     Double findMaxDividendYieldBySymbol(String symbol);
//     Double findMinPeBySymbol(String symbol);
// }
