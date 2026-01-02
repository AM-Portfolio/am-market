package com.am.marketdata.processor.service.mapper;

import java.time.LocalDateTime;
import java.util.UUID;

import com.am.common.investment.model.equity.financial.BaseModel;
import com.am.common.investment.model.stockindice.AuditData;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Mapper for converting between BoardOfDirectors and BoardOfDirector
 */
@Component
@Slf4j
public class BaseModelMapper {
    
    public BaseModel getBaseModel(String symbol, String source) {
        return BaseModel.builder()
        .id(UUID.randomUUID().toString())
        .symbol(symbol)
        .source(source)
        .audit(getAudit())
        .build();
    }

    private AuditData getAudit() {
        return AuditData.builder()
        .createdBy("Munish")
        .createdAt(LocalDateTime.now())
        .updatedBy("Munish")
        .updatedAt(LocalDateTime.now())
        .build();
    }
}
