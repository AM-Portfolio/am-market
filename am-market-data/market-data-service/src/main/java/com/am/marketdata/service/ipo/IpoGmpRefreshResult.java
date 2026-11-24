package com.am.marketdata.service.ipo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IpoGmpRefreshResult {
    String status;
    int received;
    int matched;
    int updated;
    int duplicates;
    int unmatched;
    int skipped;
    String error;
}
