package com.am.marketdata.api.model.ipo;

import com.am.marketdata.service.model.IpoIssueDocument;
import com.am.marketdata.service.model.IpoSubscriptionCategoryEmbedded;
import com.am.marketdata.service.model.IpoSubscriptionEmbedded;
import com.am.marketdata.service.model.IpoSyncMetaDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class IpoApiMapper {

    private IpoApiMapper() {}

    public static IpoIssueListResponse toListResponse(List<IpoIssueDocument> docs, boolean includeSubscription) {
        List<IpoIssueView> data = docs.stream()
                .map(doc -> toIssueView(doc, includeSubscription))
                .collect(Collectors.toList());
        return IpoIssueListResponse.builder().data(data).count(data.size()).build();
    }

    public static IpoIssueView toIssueView(IpoIssueDocument doc, boolean includeSubscription) {
        IpoIssueView.IpoIssueViewBuilder b = IpoIssueView.builder()
                .id(doc.getId())
                .symbol(doc.getSymbol())
                .slug(doc.getSlug())
                .companyName(doc.getCompanyName())
                .series(doc.getSeries())
                .lifecycle(doc.getLifecycle())
                .status(doc.getStatus())
                .window(IpoIssueWindow.builder()
                        .openDate(doc.getOpenDate())
                        .closeDate(doc.getCloseDate())
                        .listingDate(doc.getListingDate())
                        .linkRemovalDate(doc.getLinkRemovalDate())
                        .build())
                .pricing(IpoIssuePricing.builder()
                        .currency(doc.getCurrency())
                        .priceMin(doc.getPriceMin())
                        .priceMax(doc.getPriceMax())
                        .issuePrice(doc.getIssuePrice())
                        .label(doc.getPriceLabel())
                        .build())
                .size(IpoIssueSize.builder()
                        .sharesOffered(doc.getSharesOffered())
                        .issueSizeLabel(doc.getIssueSizeLabel())
                        .build())
                .exchanges(IpoExchanges.builder()
                        .nse(doc.isOnNse())
                        .bse(doc.isOnBse())
                        .build())
                .subscriptionSummary(toSummary(doc))
                .vendor(IpoVendor.builder().source(doc.getVendorSource()).build())
                .syncedAt(doc.getSyncedAt());
        if (includeSubscription && doc.getSubscription() != null) {
            b.subscription(toSubscription(doc.getId(), doc.getSymbol(), doc.getSubscription()));
        }
        return b.build();
    }

    public static IpoSubscriptionView toSubscription(
            String issueId, String symbol, IpoSubscriptionEmbedded sub) {
        return IpoSubscriptionView.builder()
                .issueId(issueId)
                .symbol(symbol)
                .series(sub.getSeries())
                .updatedAt(sub.getUpdatedAt())
                .overall(IpoSubscriptionOverall.builder()
                        .times(sub.getOverallTimes())
                        .sharesBid(sub.getOverallSharesBid())
                        .sharesOffered(sub.getOverallSharesOffered())
                        .build())
                .categories(mapCategories(sub.getCategories()))
                .build();
    }

    public static IpoSyncMetaView toSyncMeta(IpoSyncMetaDocument meta) {
        if (meta == null) {
            return null;
        }
        return IpoSyncMetaView.builder()
                .id(meta.getId())
                .lastFullSyncAt(meta.getLastFullSyncAt())
                .lastError(meta.getLastError())
                .lastCount(meta.getLastCount())
                .lastTrigger(meta.getLastTrigger())
                .source(meta.getSource())
                .build();
    }

    private static IpoSubscriptionSummary toSummary(IpoIssueDocument doc) {
        IpoSubscriptionSummary.IpoSubscriptionSummaryBuilder b = IpoSubscriptionSummary.builder()
                .times(doc.getSubscriptionTimes())
                .sharesBid(doc.getSubscriptionSharesBid())
                .sharesOffered(doc.getSubscriptionSharesOffered());
        if (doc.getSubscription() != null) {
            b.updatedAt(doc.getSubscription().getUpdatedAt());
        }
        return b.build();
    }

    private static List<IpoSubscriptionCategoryView> mapCategories(
            List<IpoSubscriptionCategoryEmbedded> cats) {
        List<IpoSubscriptionCategoryView> out = new ArrayList<>();
        if (cats == null) {
            return out;
        }
        for (IpoSubscriptionCategoryEmbedded cat : cats) {
            out.add(IpoSubscriptionCategoryView.builder()
                    .code(cat.getCode())
                    .name(cat.getName())
                    .srNo(cat.getSrNo())
                    .sharesOffered(cat.getSharesOffered())
                    .sharesBid(cat.getSharesBid())
                    .times(cat.getTimes())
                    .children(mapCategories(cat.getChildren()))
                    .build());
        }
        return out;
    }
}
