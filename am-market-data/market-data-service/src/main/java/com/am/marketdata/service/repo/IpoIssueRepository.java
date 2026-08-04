package com.am.marketdata.service.repo;

import com.am.marketdata.service.model.IpoIssueDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IpoIssueRepository extends MongoRepository<IpoIssueDocument, String> {

    List<IpoIssueDocument> findByLifecycleOrderByOpenDateDesc(String lifecycle);

    List<IpoIssueDocument> findBySymbolIgnoreCaseOrderByOpenDateDesc(String symbol);

    long countByLifecycle(String lifecycle);
}
