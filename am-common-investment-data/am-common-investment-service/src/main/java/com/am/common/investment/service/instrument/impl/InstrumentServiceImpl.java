package com.am.common.investment.service.instrument.impl;

import com.am.common.investment.model.equity.Instrument;
import com.am.common.investment.persistence.document.instrument.InstrumentDocument;
import com.am.common.investment.persistence.repository.instrument.InstrumentRepository;
import com.am.common.investment.service.instrument.InstrumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the InstrumentService interface
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstrumentServiceImpl implements InstrumentService {

    private final InstrumentRepository instrumentRepository;

    @Override
    public Instrument saveInstrument(Instrument instrument, String symbol) {
        //log.info("Saving instrument with trading symbol: {}", instrument.getTradingSymbol());
        
        InstrumentDocument document = instrumentRepository.findLatestBySymbol(symbol)
                .map(existing -> {
                    existing.setInstrument(instrument);
                    existing.setVersion(existing.getVersion() + 1);
                    if (existing.getAudit() != null) {
                        existing.getAudit().setUpdatedAt(LocalDateTime.now());
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    InstrumentDocument newDoc = InstrumentDocument.builder()
                            .instrument(instrument)
                            .symbol(symbol)
                            .build();
                    
                    // Set audit data if needed
                    // newDoc.setAudit(AuditData.builder().createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
                    
                    return newDoc;
                });
        
        InstrumentDocument savedDocument = instrumentRepository.save(document);
        return savedDocument.getInstrument();
    }

    @Override
    public Optional<Instrument> getInstrumentBySymbol(String symbol) {
        log.debug("Getting instrument by symbol: {}", symbol);
        return instrumentRepository.findLatestBySymbol(symbol)
                .map(InstrumentDocument::getInstrument);
    }

    @Override
    public Optional<Instrument> getInstrumentByTradingSymbol(String tradingSymbol) {
        log.debug("Getting instrument by trading symbol: {}", tradingSymbol);
        return instrumentRepository.findLatestByTradingSymbol(tradingSymbol)
                .map(InstrumentDocument::getInstrument);
    }

    @Override
    public Optional<Instrument> getInstrumentByIsin(String isin) {
        log.debug("Getting instrument by ISIN: {}", isin);
        return instrumentRepository.findLatestByIsin(isin)
                .map(InstrumentDocument::getInstrument);
    }

    @Override
    public List<Instrument> getInstrumentsByExchange(String exchange) {
        log.debug("Getting instruments by exchange: {}", exchange);
        return instrumentRepository.findByInstrumentExchange(exchange, 
                Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt")))
                .stream()
                .map(InstrumentDocument::getInstrument)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteInstrumentBySymbol(String symbol) {
        log.info("Deleting instrument by symbol: {}", symbol);
        instrumentRepository.findLatestBySymbol(symbol)
                .ifPresent(instrumentRepository::delete);
    }
    
    @Override
    public List<Instrument> saveAll(List<Instrument> instruments) {
        log.info("Batch saving {} instruments", instruments.size());
        
        List<Instrument> savedInstruments = new ArrayList<>();
        int batchSize = 1000; // Configure batch size
        int totalSize = instruments.size();
        int processedCount = 0;
        
        // Process in batches
        for (int i = 0; i < totalSize; i += batchSize) {
            int endIndex = Math.min(i + batchSize, totalSize);
            List<Instrument> batch = instruments.subList(i, endIndex);
            
            log.debug("Processing batch {}/{} (size: {})", 
                    (i / batchSize) + 1, 
                    (int) Math.ceil((double) totalSize / batchSize), 
                    batch.size());
            
            // Process each instrument in the current batch
            for (Instrument instrument : batch) {
                String tradingSymbol = instrument.getTradingSymbol();
                
                if (tradingSymbol == null || tradingSymbol.isEmpty()) {
                    log.warn("Skipping instrument with null or empty trading symbol");
                    continue;
                }
                
                try {
                    Instrument savedInstrument = saveInstrument(instrument, tradingSymbol);
                    savedInstruments.add(savedInstrument);
                    processedCount++;
                } catch (Exception e) {
                    log.error("Error saving instrument with trading symbol {}: {}", tradingSymbol, e.getMessage());
                    // Continue with next instrument instead of failing the entire batch
                }
            }
            
            log.info("Processed batch {}/{}, progress: {}/{} instruments ({}%)", 
                    (i / batchSize) + 1, 
                    (int) Math.ceil((double) totalSize / batchSize), 
                    processedCount, 
                    totalSize, 
                    String.format("%.2f", processedCount * 100.0 / totalSize));
        }
        
        log.info("Successfully saved {} out of {} instruments ({}%)", 
                savedInstruments.size(), 
                instruments.size(), 
                String.format("%.2f", savedInstruments.size() * 100.0 / instruments.size()));
        return savedInstruments;
    }

    @Override
    public Optional<Instrument> getInstrumentByInstrumentToken(Long instrumentToken) {
        return instrumentRepository.findLatestByInstrumentToken(instrumentToken)
                .map(InstrumentDocument::getInstrument);
    }
    
    @Override
    public List<Instrument> getInstrumentByTradingsymbols(List<String> tradingSymbols) {
        log.debug("Getting instruments by multiple trading symbols: {}", tradingSymbols);
        
        if (tradingSymbols == null || tradingSymbols.isEmpty()) {
            log.warn("Empty or null trading symbols list provided");
            return new ArrayList<>();
        }
        
        return instrumentRepository.findByInstrumentTradingSymbolIn(tradingSymbols, 
                Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt")))
                .stream()
                .map(InstrumentDocument::getInstrument)
                .collect(Collectors.toList());  
    }

    @Override
    public List<Instrument> getInstrumentByInstrumentTokens(List<Long> instrumentTokens) {
        log.debug("Getting instruments by multiple instrument tokens: {}", instrumentTokens);
        
        if (instrumentTokens == null || instrumentTokens.isEmpty()) {
            log.warn("Empty or null instrument tokens list provided");
            return new ArrayList<>();
        }
        
        return instrumentRepository.findByInstrumentInstrumentTokenIn(instrumentTokens, 
                Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt")))
                .stream()
                .map(InstrumentDocument::getInstrument)
                .collect(Collectors.toList());  
    }
}
