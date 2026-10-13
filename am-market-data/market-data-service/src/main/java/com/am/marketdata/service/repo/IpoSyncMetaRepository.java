package com.am.marketdata.service.repo;

import com.am.marketdata.service.model.IpoSyncMetaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IpoSyncMetaRepository extends MongoRepository<IpoSyncMetaDocument, String> {}
