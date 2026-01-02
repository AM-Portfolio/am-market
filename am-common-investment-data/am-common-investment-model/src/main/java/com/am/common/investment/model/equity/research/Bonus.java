package com.am.common.investment.model.equity.research;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Bonus {
    private String bonusType;
    private LocalDate announcementDate;
    private LocalDate recordDate;
    private Double bonusAmount;
    private Double bonusPercentage;
    private LocalDate exPDate;
}
