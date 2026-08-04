package com.am.marketdata.api.service;

import com.am.marketdata.api.model.ipo.IpoApiMapper;
import com.am.marketdata.api.model.ipo.IpoIssueListResponse;
import com.am.marketdata.api.model.ipo.IpoIssueView;
import com.am.marketdata.api.model.ipo.IpoSubscriptionView;
import com.am.marketdata.api.model.ipo.IpoSyncMetaView;
import com.am.marketdata.service.ipo.IpoQueryService;
import com.am.marketdata.service.ipo.IpoSyncService;
import com.am.marketdata.service.model.IpoIssueDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IpoApiService {

    private final IpoQueryService ipoQueryService;
    private final IpoSyncService ipoSyncService;

    public List<IpoSyncMetaView> syncMeta() {
        return ipoSyncService.allSyncMeta().stream()
                .map(IpoApiMapper::toSyncMeta)
                .collect(Collectors.toList());
    }

    public IpoIssueListResponse list(String lifecycle, String series, String q) {
        return IpoApiMapper.toListResponse(ipoQueryService.list(lifecycle, series, q), false);
    }

    public IpoIssueListResponse bySymbol(String symbol) {
        return IpoApiMapper.toListResponse(ipoQueryService.bySymbol(symbol), true);
    }

    public Optional<IpoIssueView> detail(String symbol, LocalDate openDate) {
        return ipoQueryService
                .bySymbolAndOpenDate(symbol, openDate)
                .map(doc -> IpoApiMapper.toIssueView(doc, true));
    }

    public Optional<IpoSubscriptionView> subscription(String symbol, LocalDate openDate) {
        return ipoQueryService.bySymbolAndOpenDate(symbol, openDate).flatMap(this::toSubscription);
    }

    private Optional<IpoSubscriptionView> toSubscription(IpoIssueDocument doc) {
        if (doc.getSubscription() == null) {
            return Optional.empty();
        }
        return Optional.of(IpoApiMapper.toSubscription(doc.getId(), doc.getSymbol(), doc.getSubscription()));
    }
}
