package com.am.marketdata.common.mapper;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.marketdata.common.model.CommonInstrument;
import com.am.marketdata.common.model.CommonQuote;
import com.am.marketdata.common.model.OHLCQuote;

import java.util.Map;

public interface ProviderResponseMapper {
    CommonQuote mapQuote(Object vendorQuote);

    OHLCQuote mapOHLC(Object vendorOHLC);

    HistoricalData mapHistoricalData(Object vendorData);

    CommonInstrument mapInstrument(Object vendorInstrument);

    CommonQuote mapLTP(Object vendorLTP);

    Map<String, CommonQuote> mapQuotes(Map<String, ?> vendorQuotes);

    Map<String, OHLCQuote> mapOHLCs(Map<String, ?> vendorOHLCs);

    Map<String, CommonQuote> mapLTPs(Map<String, ?> vendorLTPs);

    String getProviderName();
}
