package com.am.marketdata.processor.service.validator;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.board.Director;
import com.am.common.investment.model.equity.financial.cashflow.CashFlow;
import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
import com.am.marketdata.processor.service.common.DataValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Validator for NSE stock cash flow data
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockCashFlowValidator implements DataValidator<StockCashFlow> {
    
    @Value("${market.data.max.age.minutes:1500}")
    private long maxDataAgeMinutes;
    
    @Override
    public boolean isValid(StockCashFlow response) {
        if (response == null || response.getCashFlow() == null || response.getCashFlow().isEmpty()) {
            log.warn("Received empty stock cash flow response");
            return false;
        }

        // Parse and validate market status date
        try {
            LocalDateTime marketStatusDate = response.getAudit().getUpdatedAt();
            if (marketStatusDate == null) {
                log.warn("Stock cash flow response missing market status date");
                return false;
            }

            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            long minutesOld = java.time.Duration.between(marketStatusDate, now).toMinutes();

            if (minutesOld > maxDataAgeMinutes) {
                log.warn("Stock cash flow data is too old: {} minutes old", minutesOld);
                return false;
            }

            // Validate stock data
            for (CashFlow item : response.getCashFlow()) {
                if (item.getYearEnd() == null ) {
                    log.warn("Invalid stock cash flow data for symbol {}", 
                    response.getSymbol());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating stock cash flow data for symbol {}", response.getSymbol(), e);
            return false;
        }
    }
    
    @Override
    public String getDataTypeName() {
        return "stock-cash-flow";
    }
}