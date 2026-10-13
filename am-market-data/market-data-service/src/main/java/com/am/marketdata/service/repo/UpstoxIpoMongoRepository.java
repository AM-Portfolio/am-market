package com.am.marketdata.service.repo;

import com.am.marketdata.service.model.UpstoxIpoDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB Repository for managing {@link UpstoxIpoDocument} entities.
 */
@Repository
public interface UpstoxIpoMongoRepository extends MongoRepository<UpstoxIpoDocument, String> {

    /**
     * Finds documents filtered by status and issueType with pagination.
     */
    Page<UpstoxIpoDocument> findByStatusAndIssueType(String status, String issueType, Pageable pageable);

    /**
     * Finds documents filtered by status only with pagination.
     */
    Page<UpstoxIpoDocument> findByStatus(String status, Pageable pageable);

    /**
     * Finds all documents by status.
     */
    List<UpstoxIpoDocument> findByStatus(String status);

    /**
     * Counts documents by status.
     */
    long countByStatus(String status);

    /**
     * Counts documents by status and bidding end date.
     */
    long countByStatusAndBiddingEndDate(String status, String biddingEndDate);
}

