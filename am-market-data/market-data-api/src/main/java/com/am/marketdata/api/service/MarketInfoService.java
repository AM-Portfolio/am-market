package com.am.marketdata.api.service;

import com.am.marketdata.common.model.marketinfo.ChangeOiResponse;
import com.am.marketdata.common.model.marketinfo.FlowsOverviewResponse;
import com.am.marketdata.common.model.marketinfo.InstitutionalFlowResponse;
import com.am.marketdata.common.model.marketinfo.OiResponse;

import java.util.List;

public interface MarketInfoService {
    InstitutionalFlowResponse getFii(List<String> dataTypes, String interval, String from);

    InstitutionalFlowResponse getDii(String interval, String from);

    OiResponse getOi(String symbol, String expiry, String date);

    ChangeOiResponse getChangeOi(String symbol, String expiry, String date, int intervalDays);

    FlowsOverviewResponse getFlowsOverview(String interval);
}
