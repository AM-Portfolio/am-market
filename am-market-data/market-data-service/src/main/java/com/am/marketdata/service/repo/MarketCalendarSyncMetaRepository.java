package com.am.marketdata.service.repo;

import com.am.marketdata.service.model.MarketCalendarSyncMetaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MarketCalendarSyncMetaRepository extends MongoRepository<MarketCalendarSyncMetaDocument, String> {
}
