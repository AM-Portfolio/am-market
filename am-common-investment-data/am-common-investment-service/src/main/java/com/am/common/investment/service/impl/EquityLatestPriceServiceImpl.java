package com.am.common.investment.service.impl;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.persistence.repository.measurement.EquityLatestPriceMeasurementRepository;
import com.am.common.investment.service.EquityLatestPriceService;
import com.am.common.investment.service.mapper.EquityPriceMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dedicated Service Implementation for Fast Latest-Price Batch Lookups.
 * 
 * WHAT PROBLEM IT SOLVES:
 * Bypasses 30-day historical time-series scans used by standard EquityServiceImpl, executing
 * narrow range queries specifically optimized for OHLC endpoints and market quote lookups.
 * 
 * HOW IT WORKS:
 * Uses EquityLatestPriceMeasurementRepository with range(start: -5d) window to return latest
 * price points in ~35ms instead of 10,000ms.
 */
@Service
@RequiredArgsConstructor
public class EquityLatestPriceServiceImpl implements EquityLatestPriceService {

    private static final Logger logger = LoggerFactory.getLogger(EquityLatestPriceServiceImpl.class);

    private final EquityLatestPriceMeasurementRepository latestPriceRepository;
    private final EquityPriceMapper mapper;

    @Override
    public List<EquityPrice> getLatestPricesByTradingSymbols(List<String> tradingSymbols) {
        if (tradingSymbols == null || tradingSymbols.isEmpty()) {
            logger.warn("getLatestPricesByTradingSymbols: Empty or null trading symbols list provided");
            return new ArrayList<>();
        }

        logger.info("Fetching fast latest prices for {} trading symbols (-5d window)", tradingSymbols.size());
        long startTime = System.currentTimeMillis();

        List<EquityPrice> prices = latestPriceRepository.findLatestPricesByTradingSymbolIn(tradingSymbols)
                .stream()
                .map(mapper::toModel)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved {} latest prices out of {} requested symbols in {}ms", prices.size(), tradingSymbols.size(), duration);

        return prices;
    }

    @Override
    public List<EquityPrice> getLatestPricesByIsin(List<String> isins) {
        if (isins == null || isins.isEmpty()) {
            logger.warn("getLatestPricesByIsin: Empty or null ISINs list provided");
            return new ArrayList<>();
        }

        logger.info("Fetching fast latest prices for {} ISINs (-5d window)", isins.size());
        long startTime = System.currentTimeMillis();

        List<EquityPrice> prices = latestPriceRepository.findLatestPricesByIsinIn(isins)
                .stream()
                .map(mapper::toModel)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved {} latest ISIN prices out of {} requested ISINs in {}ms", prices.size(), isins.size(), duration);

        return prices;
    }
}
