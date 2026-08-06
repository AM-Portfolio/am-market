package com.am.marketdata.service.repo;

import com.am.marketdata.service.model.MarketCalendarDayDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketCalendarRepository extends MongoRepository<MarketCalendarDayDocument, String> {

    List<MarketCalendarDayDocument> findByExchangeAndDateBetween(
            String exchange, LocalDate startInclusive, LocalDate endInclusive);

    Optional<MarketCalendarDayDocument> findByExchangeAndDate(String exchange, LocalDate date);

    long countByExchangeAndDateBetween(String exchange, LocalDate startInclusive, LocalDate endInclusive);
}
