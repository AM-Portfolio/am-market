package com.am.marketdata.service.repo;

import com.am.marketdata.common.model.UnresolvedSymbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;

public class UnresolvedSymbolRepositoryCustomImpl implements UnresolvedSymbolRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void incrementRequestCount(String symbol) {
        Query query = new Query(Criteria.where("symbol").is(symbol));
        Update update = new Update()
                .setOnInsert("first_requested_at", new Date())
                .set("last_requested_at", new Date())
                .inc("request_count", 1)
                .setOnInsert("resolved", false);

        mongoTemplate.upsert(query, update, UnresolvedSymbol.class);
    }
}
