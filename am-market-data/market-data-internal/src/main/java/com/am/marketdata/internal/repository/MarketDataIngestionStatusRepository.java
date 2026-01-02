package com.am.marketdata.internal.repository;

import com.am.marketdata.internal.model.MarketDataIngestionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketDataIngestionStatusRepository extends MongoRepository<MarketDataIngestionStatus, String> {
}
