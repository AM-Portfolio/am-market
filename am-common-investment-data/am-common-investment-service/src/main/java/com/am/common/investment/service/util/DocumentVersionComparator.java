package com.am.common.investment.service.util;

import com.am.common.investment.persistence.document.BaseDocument;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for comparing and selecting the latest version of documents
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentVersionComparator {

    /**
     * Comparator that first compares document version and then update time
     */
    private static final Comparator<BaseDocument> VERSION_COMPARATOR = Comparator
            .comparing(BaseDocument::getVersion)
            .thenComparing(doc -> doc.getAudit().getUpdatedAt());

    /**
     * Get the latest document from a list based on version and update time
     *
     * @param documents List of documents to compare
     * @param <T>      Type of document
     * @return Optional containing the latest document if found
     */
    public static <T extends BaseDocument> Optional<T> getLatestDocument(List<T> documents) {
        if (documents == null || documents.isEmpty()) {
            return Optional.empty();
        }
        
        return documents.stream()
                .max(VERSION_COMPARATOR);
    }

    /**
     * Get the latest document from a list based on version and update time
     *
     * @param documents List of documents to compare
     * @param <T>      Type of document
     * @return The latest document if found, null otherwise
     */
    public static <T extends BaseDocument> T getLatestDocumentOrNull(List<T> documents) {
        return getLatestDocument(documents).orElse(null);
    }

    /**
     * Sort a list of documents by version and update time
     *
     * @param documents List of documents to sort
     * @param <T>      Type of document
     * @return Sorted list of documents
     */
    public static <T extends BaseDocument> List<T> sortDocumentsByVersion(List<T> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }
        
        return documents.stream()
                .sorted(VERSION_COMPARATOR)
                .toList();
    }

    /**
     * Create a Sort object for MongoDB queries that sorts by version and update time
     *
     * @return Sort object for MongoDB queries
     */
    public static Sort getSortByVersionAndTime() {
        return Sort.by(Sort.Order.desc("version"), Sort.Order.desc("audit.updatedAt"));
    }
}
