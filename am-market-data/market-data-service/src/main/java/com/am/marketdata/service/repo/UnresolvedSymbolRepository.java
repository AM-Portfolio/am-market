package com.am.marketdata.service.repo;

import com.am.marketdata.common.model.UnresolvedSymbol;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnresolvedSymbolRepository extends MongoRepository<UnresolvedSymbol, String>, UnresolvedSymbolRepositoryCustom {
}
