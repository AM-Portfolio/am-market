package com.am.common.investment.persistence.repository.financial;

import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.am.common.investment.persistence.document.BaseDocument;

/**
 * Generic repository interface for financial documents with version and time sorting
 */
public interface BaseDocumentRepository<T extends BaseDocument, ID> extends MongoRepository<T, String> {
    
    Optional<T> findBySymbol(String symbol, Sort sort);

    default Optional<T> findBySymbolWithVersionAndTime(String symbol) {
        return findBySymbol(symbol, Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt")));
    }
}
