package com.am.marketdata.api.service;

import com.am.marketdata.api.model.FundamentalAnalysisResponse;
import com.am.marketdata.api.model.FundamentalRatiosResponse;
import com.am.marketdata.common.model.fundamental.CompetitorPeer;
import com.am.marketdata.common.model.fundamental.CorporateActionEntry;
import com.am.marketdata.common.model.fundamental.FundamentalAnalytics;
import com.am.marketdata.common.model.fundamental.ShareholdingQuarterEntry;

import java.util.List;

public interface FundamentalAnalysisService {

    default FundamentalAnalysisResponse getFundamentals(String symbol) {
        return getFundamentals(symbol, "NSE");
    }

    FundamentalAnalysisResponse getFundamentals(String symbol, String exchange);

    default FundamentalAnalysisResponse.CompanyOverviewSection getCompanyProfile(String symbol) {
        return getCompanyProfile(symbol, "NSE");
    }

    FundamentalAnalysisResponse.CompanyOverviewSection getCompanyProfile(String symbol, String exchange);

    default FundamentalRatiosResponse getRatios(String symbol) {
        return getRatios(symbol, "NSE");
    }

    FundamentalRatiosResponse getRatios(String symbol, String exchange);

    FundamentalAnalysisResponse.FinancialsSection getFinancials(String symbol);

    List<ShareholdingQuarterEntry> getShareholding(String symbol);

    List<CorporateActionEntry> getCorporateActions(String symbol);

    List<CompetitorPeer> getPeers(String symbol);

    FundamentalAnalytics getAnalytics(String symbol);
}
