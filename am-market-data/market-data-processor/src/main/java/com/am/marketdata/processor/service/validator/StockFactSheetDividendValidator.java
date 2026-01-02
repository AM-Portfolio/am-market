package com.am.marketdata.processor.service.validator;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.board.Director;
import com.am.common.investment.model.equity.financial.factsheetdividend.FactSheetDividend;
import com.am.common.investment.model.equity.financial.factsheetdividend.StockFactSheetDividend;
import com.am.marketdata.processor.service.common.DataValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockFactSheetDividendValidator implements DataValidator<StockFactSheetDividend> {
    
    @Value("${market.data.max.age.minutes:1500}")
    private long maxDataAgeMinutes;
    
    @Override
    public boolean isValid(StockFactSheetDividend response) {
        if (response == null || response.getFactSheetDividend() == null || response.getFactSheetDividend().isEmpty()) {
            log.warn("Received empty stock dividend response");
            return false;
        }

        // Parse and validate market status date
        try {
            LocalDateTime marketStatusDate = response.getAudit().getUpdatedAt();
            if (marketStatusDate == null) {
                log.warn("Stock dividend response missing market status date");
                return false;
            }

            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            long minutesOld = java.time.Duration.between(marketStatusDate, now).toMinutes();

            if (minutesOld > maxDataAgeMinutes) {
                log.warn("Stock dividend data is too old: {} minutes old", minutesOld);
                return false;
            }

            // Validate stock data
            for (FactSheetDividend item : response.getFactSheetDividend()) {
                if (item.getYearEnd() == null ) {
                    log.warn("Invalid stock dividend data for symbol {}", 
                    response.getSymbol());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating stock dividend data for symbol {}", response.getSymbol(), e);
            return false;
        }
    }
    
    @Override
    public String getDataTypeName() {
        return "stock-dividend";
    }
}