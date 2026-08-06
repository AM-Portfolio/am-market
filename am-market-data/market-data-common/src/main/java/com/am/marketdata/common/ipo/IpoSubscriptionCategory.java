package com.am.marketdata.common.ipo;

import java.util.List;
import java.util.Objects;

public final class IpoSubscriptionCategory {
    private final String code;
    private final String name;
    private final String srNo;
    private final Long sharesOffered;
    private final Long sharesBid;
    private final Double times;
    private final List<IpoSubscriptionCategory> children;

    public IpoSubscriptionCategory(
            String code,
            String name,
            String srNo,
            Long sharesOffered,
            Long sharesBid,
            Double times,
            List<IpoSubscriptionCategory> children) {
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.srNo = srNo;
        this.sharesOffered = sharesOffered;
        this.sharesBid = sharesBid;
        this.times = times;
        this.children = children == null ? List.of() : List.copyOf(children);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSrNo() {
        return srNo;
    }

    public Long getSharesOffered() {
        return sharesOffered;
    }

    public Long getSharesBid() {
        return sharesBid;
    }

    public Double getTimes() {
        return times;
    }

    public List<IpoSubscriptionCategory> getChildren() {
        return children;
    }
}
