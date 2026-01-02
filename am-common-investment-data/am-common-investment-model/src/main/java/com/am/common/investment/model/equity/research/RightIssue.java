package com.am.common.investment.model.equity.research;

import java.time.LocalDate;

import lombok.Data;

@Data
 class RightIssue {
    private String rightIssueType;
    private LocalDate announcementDate;
    private LocalDate recordDate;
    private Double rightIssuePrice;
    private Double rightIssueRatio;
    private LocalDate exRightDate;
}