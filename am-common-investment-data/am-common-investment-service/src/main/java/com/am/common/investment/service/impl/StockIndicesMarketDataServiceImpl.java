package com.am.common.investment.service.impl;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.model.stockindice.AuditData;
import com.am.common.investment.persistence.document.StockIndicesMarketDataDocument;
import com.am.common.investment.persistence.repository.StockIndicesMarketDataRepository;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.common.investment.service.mapper.StockIndicesMarketDataMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the StockIndicesMarketDataService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndicesMarketDataServiceImpl implements StockIndicesMarketDataService {

    private final StockIndicesMarketDataRepository repository;
    private final StockIndicesMarketDataMapper mapper;

    @Override
    public StockIndicesMarketData save(StockIndicesMarketData marketData) {
        log.debug("Saving stock indices market data: {}", marketData.getIndexSymbol());
        
        StockIndicesMarketDataDocument document = mapper.toDocument(marketData);
        
        // Set audit fields if not already set
        if (document.getAudit() == null) {
            document.setAudit(new AuditData());
        }
        if (document.getAudit().getCreatedAt() == null) {
            document.getAudit().setCreatedAt(LocalDateTime.now());
        }
        document.getAudit().setUpdatedAt(LocalDateTime.now());

        StockIndicesMarketDataDocument saved = repository.save(document);
        return mapper.toModel(saved);
    }

    @Override
    public StockIndicesMarketData findByIndexSymbol(String symbol) {
        log.debug("Finding stock indices market data for symbol: {}", symbol);
        
        StockIndicesMarketDataDocument document = repository.findByIndexSymbol(symbol);
        
        if (document != null) {
            return mapper.toModel(document);
        }
        return null;
    }

    @Override
    public List<StockIndicesMarketData> findByIndexSymbols(Set<String> symbols) {
        log.debug("Finding stock indices market data for symbols: {}", symbols);
        
        List<StockIndicesMarketDataDocument> documents = repository.findByIndexSymbolIn(symbols);
        
        return documents.stream()
            .map(mapper::toModel)
            .collect(Collectors.toList());
    }
}
