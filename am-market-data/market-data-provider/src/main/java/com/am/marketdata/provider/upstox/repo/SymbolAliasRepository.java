package com.am.marketdata.provider.upstox.repo;

import com.am.marketdata.common.model.SymbolAlias;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SymbolAliasRepository extends MongoRepository<SymbolAlias, String> {
    List<SymbolAlias> findByType(String type);
}
