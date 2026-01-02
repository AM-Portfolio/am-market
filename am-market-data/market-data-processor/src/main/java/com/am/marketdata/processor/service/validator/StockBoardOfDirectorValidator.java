package com.am.marketdata.processor.service.validator;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.board.Director;
import com.am.marketdata.processor.service.common.DataValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Validator for NSE stock board of directors data
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockBoardOfDirectorValidator implements DataValidator<BoardOfDirectors> {
    
    @Value("${market.data.max.age.minutes:1500}")
    private long maxDataAgeMinutes;
    
    @Override
    public boolean isValid(BoardOfDirectors response) {
        if (response == null || response.getDirectors() == null || response.getDirectors().isEmpty()) {
            log.warn("Received empty stock board of directors response");
            return false;
        }

        // Parse and validate market status date
        try {
            LocalDateTime marketStatusDate = response.getAudit().getUpdatedAt();
            if (marketStatusDate == null) {
                log.warn("Stock board of directors response missing market status date");
                return false;
            }

            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            long minutesOld = java.time.Duration.between(marketStatusDate, now).toMinutes();

            if (minutesOld > maxDataAgeMinutes) {
                log.warn("Stock board of directors data is too old: {} minutes old", minutesOld);
                return false;
            }

            // Validate stock data
            for (Director director : response.getDirectors()) {
                if (director.getCompanyId() == null || 
                    director.getReportedDsg() == null) {
                    log.warn("Invalid stock board of directors data for symbol {}", 
                        director.getCompanyId());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating stock board of directors data for symbol {}", response.getSymbol(), e);
            return false;
        }
    }
    
    @Override
    public String getDataTypeName() {
        return "stock-indices";
    }
}