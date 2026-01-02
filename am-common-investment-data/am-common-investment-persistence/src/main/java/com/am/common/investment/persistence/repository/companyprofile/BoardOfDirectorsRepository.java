package com.am.common.investment.persistence.repository.companyprofile;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.am.common.investment.persistence.document.companyprofile.BoardOfDirectorsDocument;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing BoardOfDirectorsDocument in MongoDB
 */
@Repository
public interface BoardOfDirectorsRepository extends MongoRepository<BoardOfDirectorsDocument, String> {
    List<BoardOfDirectorsDocument> findBySymbol(String symbol, Sort sort);

    default Optional<BoardOfDirectorsDocument> findBySymbolWithVersionAndTime(String symbol) {
        return findBySymbol(symbol, Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt"))).stream().findFirst();
    }
}
