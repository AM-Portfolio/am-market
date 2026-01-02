package com.am.common.investment.model.equity.financial;

import java.util.UUID;

import com.am.common.investment.model.stockindice.AuditData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseModel {
    private String id;
    private String symbol;
    private Integer version;
    private AuditData audit;
    private String source;
}
