package com.am.common.investment.persistence.repository.stock.financial;

import java.util.List;
import java.util.Optional;

import com.am.common.investment.persistence.document.stock.financial.balancesheet.BalanceSheetDocument;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceSheetRepository extends MongoRepository<BalanceSheetDocument, String> {
    List<BalanceSheetDocument> findBySymbol(String symbol, Sort sort);

    default Optional<BalanceSheetDocument> findBySymbolWithVersionAndTime(String symbol) {
        return findBySymbol(symbol, Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt"))).stream().findFirst();
    }
}
