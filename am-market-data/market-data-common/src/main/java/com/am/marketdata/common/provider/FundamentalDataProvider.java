package com.am.marketdata.common.provider;

import com.am.marketdata.common.model.fundamental.BalanceSheetEntry;
import com.am.marketdata.common.model.fundamental.CashFlowEntry;
import com.am.marketdata.common.model.fundamental.CompanyProfile;
import com.am.marketdata.common.model.fundamental.CompetitorPeer;
import com.am.marketdata.common.model.fundamental.CorporateActionEntry;
import com.am.marketdata.common.model.fundamental.IncomeStatementEntry;
import com.am.marketdata.common.model.fundamental.KeyRatios;
import com.am.marketdata.common.model.fundamental.ShareholdingQuarterEntry;

import java.util.List;

/**
 * Provider-agnostic contract for retrieving fundamental analysis data of listed companies.
 * Implementations (e.g., Upstox, Zerodha) handle third-party specifics and data mapping.
 */
public interface FundamentalDataProvider {

    /**
     * Unique identifier for the provider (e.g., "UPSTOX").
     */
    String getProviderName();

    /**
     * Fetches company overview, description, and sector market cap.
     *
     * @param isin International Securities Identification Number.
     * @return CompanyProfile domain object.
     */
    CompanyProfile getCompanyProfile(String isin);

    /**
     * Fetches valuation and profitability key ratios (P/E, P/B, ROE, ROCE, EV/EBITDA, etc.).
     *
     * @param isin International Securities Identification Number.
     * @return KeyRatios domain object.
     */
    KeyRatios getKeyRatios(String isin);

    /**
     * Fetches historical Income Statements (P&L).
     *
     * @param isin International Securities Identification Number.
     * @param consolidated whether to fetch consolidated statements; falls back to standalone if unavailable.
     * @param timePeriod "yearly" or "quarterly".
     * @return List of IncomeStatementEntry items ordered chronologically.
     */
    List<IncomeStatementEntry> getIncomeStatement(String isin, boolean consolidated, String timePeriod);

    /**
     * Fetches historical Balance Sheet statements.
     *
     * @param isin International Securities Identification Number.
     * @param consolidated whether to fetch consolidated statements; falls back to standalone if unavailable.
     * @return List of BalanceSheetEntry items ordered chronologically.
     */
    List<BalanceSheetEntry> getBalanceSheet(String isin, boolean consolidated);

    /**
     * Fetches historical Cash Flow statements.
     *
     * @param isin International Securities Identification Number.
     * @param consolidated whether to fetch consolidated statements; falls back to standalone if unavailable.
     * @return List of CashFlowEntry items ordered chronologically.
     */
    List<CashFlowEntry> getCashFlow(String isin, boolean consolidated);

    /**
     * Fetches quarterly historical shareholding patterns (Promoters, FII, DII, Mutual Funds, Retail).
     *
     * @param isin International Securities Identification Number.
     * @return List of ShareholdingQuarterEntry items ordered chronologically.
     */
    List<ShareholdingQuarterEntry> getShareHoldings(String isin);

    /**
     * Fetches historical corporate actions (Dividends, Bonus, Stock Splits, Rights).
     *
     * @param isin International Securities Identification Number.
     * @return List of CorporateActionEntry items.
     */
    List<CorporateActionEntry> getCorporateActions(String isin);

    /**
     * Fetches industry competitor instrument keys and basic sector profiles.
     *
     * @param isin International Securities Identification Number.
     * @return List of CompetitorPeer items.
     */
    List<CompetitorPeer> getCompetitors(String isin);
}
