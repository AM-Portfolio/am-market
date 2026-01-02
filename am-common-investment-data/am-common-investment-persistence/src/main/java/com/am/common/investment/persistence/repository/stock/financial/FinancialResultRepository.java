package com.am.common.investment.persistence.repository.stock.financial;

import java.util.List;
import java.util.Optional;

import com.am.common.investment.persistence.document.stock.financial.result.FinancialResultDocument;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialResultRepository extends MongoRepository<FinancialResultDocument, String> {
    List<FinancialResultDocument> findBySymbol(String symbol, Sort sort);

    default Optional<FinancialResultDocument> findBySymbolWithVersionAndTime(String symbol) {
        return findBySymbol(symbol, Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt"))).stream().findFirst();
    }
}
