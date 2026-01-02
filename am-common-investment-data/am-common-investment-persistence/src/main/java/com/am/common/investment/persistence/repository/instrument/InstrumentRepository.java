package com.am.common.investment.persistence.repository.instrument;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.am.common.investment.persistence.document.instrument.InstrumentDocument;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing InstrumentDocument in MongoDB
 */
@Repository
public interface InstrumentRepository extends MongoRepository<InstrumentDocument, String> {
    List<InstrumentDocument> findBySymbol(String symbol, Sort sort);
    
    List<InstrumentDocument> findByInstrumentTradingSymbol(String tradingSymbol, Sort sort);
    
    List<InstrumentDocument> findByInstrumentIsin(String isin, Sort sort);
    
    List<InstrumentDocument> findByInstrumentExchange(String exchange, Sort sort);
    
    List<InstrumentDocument> findByInstrumentInstrumentToken(Long instrumentToken, Sort sort);
    
    List<InstrumentDocument> findByInstrumentTradingSymbolIn(List<String> tradingSymbols, Sort sort);
    
    List<InstrumentDocument> findByInstrumentInstrumentTokenIn(List<Long> instrumentTokens, Sort sort);
    
    default Optional<InstrumentDocument> findLatestBySymbol(String symbol) {
        return findBySymbol(symbol, Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt"))).stream().findFirst();
    }
    
    default Optional<InstrumentDocument> findLatestByTradingSymbol(String tradingSymbol) {
        return findByInstrumentTradingSymbol(tradingSymbol, Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt"))).stream().findFirst();
    }
    
    default Optional<InstrumentDocument> findLatestByIsin(String isin) {
        return findByInstrumentIsin(isin, Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt"))).stream().findFirst();
    }
    
    default Optional<InstrumentDocument> findLatestByInstrumentToken(Long instrumentToken) {
        return findByInstrumentInstrumentToken(instrumentToken, Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt"))).stream().findFirst();
    }
}
