package com.am.marketdata.processor.service.validator;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.board.Director;
import com.am.common.investment.model.equity.financial.balancesheet.BalanceSheet;
import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
import com.am.marketdata.processor.service.common.DataValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Validator for NSE stock balance sheet data
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockBalanceSheetValidator implements DataValidator<StockBalanceSheet> {
    
    @Value("${market.data.max.age.minutes:1500}")
    private long maxDataAgeMinutes;
    
    @Override
    public boolean isValid(StockBalanceSheet response) {
        if (response == null || response.getBalanceSheet() == null || response.getBalanceSheet().isEmpty()) {
            log.warn("Received empty stock balance sheet response");
            return false;
        }

        // Parse and validate market status date
        try {
            LocalDateTime marketStatusDate = response.getAudit().getUpdatedAt();
            if (marketStatusDate == null) {
                log.warn("Stock balance sheet response missing market status date");
                return false;
            }

            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            long minutesOld = java.time.Duration.between(marketStatusDate, now).toMinutes();

            if (minutesOld > maxDataAgeMinutes) {
                log.warn("Stock balance sheet data is too old: {} minutes old", minutesOld);
                return false;
            }

            // Validate stock data
            for (BalanceSheet item : response.getBalanceSheet()) {
                if (item.getYearEnd() == null ) {
                    log.warn("Invalid stock balance sheet data for symbol {}", 
                    response.getSymbol());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating stock balance sheet data for symbol {}", response.getSymbol(), e);
            return false;
        }
    }
    
    @Override
    public String getDataTypeName() {
        return "stock-balance-sheet";
    }
}