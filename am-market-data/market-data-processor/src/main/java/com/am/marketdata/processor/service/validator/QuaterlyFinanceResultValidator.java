package com.am.marketdata.processor.service.validator;

import com.am.common.investment.model.equity.financial.resultstatement.FinancialResult;
import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
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
public class QuaterlyFinanceResultValidator implements DataValidator<QuaterlyResult> {
    
    @Value("${market.data.max.age.minutes:1500}")
    private long maxDataAgeMinutes;
    
    @Override
    public boolean isValid(QuaterlyResult response) {
        if (response == null || response.getFinancialResults() == null || response.getFinancialResults().isEmpty()) {
            log.warn("Received empty stock financial results response");
            return false;
        }

        // Parse and validate market status date
        try {
            LocalDateTime marketStatusDate = response.getAudit().getUpdatedAt();
            if (marketStatusDate == null) {
                log.warn("Stock financial results response missing market status date");
                return false;
            }

            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            long minutesOld = java.time.Duration.between(marketStatusDate, now).toMinutes();

            if (minutesOld > maxDataAgeMinutes) {
                log.warn("Stock financial results data is too old: {} minutes old", minutesOld);
                return false;
            }

            // Validate stock data
            for (FinancialResult financialResult : response.getFinancialResults()) {
                if (financialResult.getYearEnd() == null ) {
                    log.warn("Invalid stock financial results data for symbol {}", 
                    response.getSymbol());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating stock financial results data for symbol {}", response.getSymbol(), e);
            return false;
        }
    }
    
    @Override
    public String getDataTypeName() {
        return "stock-financial-results";
    }
}