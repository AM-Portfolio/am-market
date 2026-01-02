package com.am.common.investment.persistence.repository.stock.financial;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.am.common.investment.persistence.document.stock.financial.profitandloss.ProfitAndLossDocument;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfitAndLossRepository extends MongoRepository<ProfitAndLossDocument, String> {
    List<ProfitAndLossDocument> findBySymbol(String symbol, Sort sort);

    default Optional<ProfitAndLossDocument> findBySymbolWithVersionAndTime(String symbol) {
        return findBySymbol(symbol, Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt"))).stream().findFirst();
    }
}
