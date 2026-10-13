package com.am.marketdata.common.ipo;

import java.util.List;
import java.util.Optional;

public interface IpoIssueSource {

    String sourceId();

    List<IpoIssue> fetchPast();

    List<IpoIssue> fetchCurrent();

    List<IpoIssue> fetchUpcoming();

    Optional<IpoSubscription> fetchSubscription(String symbol, String series);
}
