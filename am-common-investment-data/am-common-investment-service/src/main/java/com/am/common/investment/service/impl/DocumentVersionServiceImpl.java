package com.am.common.investment.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.am.common.investment.persistence.document.BaseDocument;
import com.am.common.investment.persistence.repository.financial.BaseDocumentRepository;
import com.am.common.investment.service.DocumentVersionService;

import java.util.UUID;

/**
 * Implementation of document version management service
 */
@Service
public class DocumentVersionServiceImpl<T extends BaseDocument> implements DocumentVersionService<T> {
    
    private static final Integer INITIAL_VERSION = 1;
    
    @Autowired
    private BaseDocumentRepository<T, UUID> baseDocumentRepository;
    
    @Override
    public Integer getNextVersion(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        
        // Find the latest version for this symbol
        Integer latestVersion = baseDocumentRepository.findBySymbol(symbol, Sort.by(Sort.Order.desc("version")))
            .map(doc -> doc.getVersion())
            .orElse(INITIAL_VERSION);
            
        // Return the next version number (increment by one)
        return latestVersion + 1;
    }
    
    @Override
    public T incrementVersion(T document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        
        // Get the next version number based on the symbol
        Integer nextVersion = getNextVersion(document.getSymbol());
        
        // Set the new version
        document.setVersion(nextVersion);
        
        return document;
    }
    
    @Override
    public Integer getInitialVersion() {
        return INITIAL_VERSION;
    }
}
