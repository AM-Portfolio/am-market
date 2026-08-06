package com.am.marketdata.service.ipo;

import com.am.marketdata.service.model.IpoIssueDocument;
import com.am.marketdata.service.repo.IpoIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IpoQueryService {

    private final IpoIssueRepository ipoIssueRepository;

    public List<IpoIssueDocument> list(String lifecycle, String series, String q) {
        List<IpoIssueDocument> base;
        if (lifecycle != null && !lifecycle.isBlank()) {
            base = ipoIssueRepository.findByLifecycleOrderByOpenDateDesc(lifecycle.trim().toUpperCase(Locale.ROOT));
        } else {
            base = ipoIssueRepository.findAll();
            base.sort((a, b) -> {
                LocalDate da = a.getOpenDate();
                LocalDate db = b.getOpenDate();
                if (da == null && db == null) {
                    return 0;
                }
                if (da == null) {
                    return 1;
                }
                if (db == null) {
                    return -1;
                }
                return db.compareTo(da);
            });
        }
        return base.stream()
                .filter(doc -> series == null
                        || series.isBlank()
                        || series.equalsIgnoreCase(doc.getSeries()))
                .filter(doc -> matchesQuery(doc, q))
                .collect(Collectors.toList());
    }

    public List<IpoIssueDocument> bySymbol(String symbol) {
        return ipoIssueRepository.findBySymbolIgnoreCaseOrderByOpenDateDesc(symbol.trim());
    }

    public Optional<IpoIssueDocument> bySymbolAndOpenDate(String symbol, LocalDate openDate) {
        return ipoIssueRepository.findById(IpoIssueDocument.idFor(symbol, openDate));
    }

    private static boolean matchesQuery(IpoIssueDocument doc, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.trim().toLowerCase(Locale.ROOT);
        return (doc.getSymbol() != null && doc.getSymbol().toLowerCase(Locale.ROOT).contains(needle))
                || (doc.getCompanyName() != null
                        && doc.getCompanyName().toLowerCase(Locale.ROOT).contains(needle));
    }
}
