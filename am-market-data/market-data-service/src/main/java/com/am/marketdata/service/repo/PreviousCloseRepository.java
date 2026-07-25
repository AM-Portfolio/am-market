package com.am.marketdata.service.repo;

import com.am.marketdata.service.model.PreviousCloseDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PreviousCloseRepository extends MongoRepository<PreviousCloseDocument, String> {

    List<PreviousCloseDocument> findBySymbolIn(Collection<String> symbols);
}
