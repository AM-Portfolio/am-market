package com.marketdata.service.margin;

import com.marketdata.common.model.margin.BrokerageCalculationRequest;
import com.marketdata.common.model.margin.BrokerageCalculationResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * Service for calculating brokerage, taxes, and other charges for stock trades
 * Implements resilient patterns including metrics and async processing
 */
@Slf4j
@Service
public class BrokerageCalculatorService {

    private final MeterRegistry meterRegistry;
    private final ThreadPoolExecutor threadPoolExecutor;
    
    // Tax and fee rates
    @Value("${market.brokerage.stt.delivery:0.1}")
    private double sttDeliveryPercent;
    
    @Value("${market.brokerage.stt.intraday:0.025}")
    private double sttIntradayPercent;
    
    @Value("${market.brokerage.gst:18}")
    private double gstPercent;
    
    @Value("${market.brokerage.exchange.nse:0.00325}")
    private double nseExchangeChargePercent;
    
    @Value("${market.brokerage.exchange.bse:0.00275}")
    private double bseExchangeChargePercent;
    
    @Value("${market.brokerage.sebi:0.0001}")
    private double sebiChargePercent;
    
    @Value("${market.brokerage.dp.charge:13.5}")
    private double dpCharge;
    
    // Stamp duty rates
    @Value("${market.brokerage.stamp.delivery:0.015}")
    private double stampDutyDeliveryPercent;
    
    @Value("${market.brokerage.stamp.intraday:0.003}")
    private double stampDutyIntradayPercent;
    
    // Default broker fees
    @Value("${market.brokerage.discount.flat-fee:20}")
    private double discountBrokerFlatFee;
    
    @Value("${market.brokerage.full-service.percentage:0.5}")
    private double fullServiceBrokerPercentage;
    
    // State-specific stamp duty rates (can be extended as needed)
    private final Map<String, Double> stateStampDutyRates = new HashMap<>();

    // Broker-specific fees from configuration
    private final Map<String, Double> brokerFees = new HashMap<>();
    
    public BrokerageCalculatorService(
            MeterRegistry meterRegistry,
            ThreadPoolExecutor threadPoolExecutor) {
        this.meterRegistry = meterRegistry;
        this.threadPoolExecutor = threadPoolExecutor;
        initializeStateStampDutyRates();
        initializeBrokerFees();
        log.info("Initializing Brokerage Calculator Service with {} broker configurations", brokerFees.size());
    }
    
    /**
     * Initialize state-specific stamp duty rates
     * These can be moved to configuration if needed
     */
    private void initializeStateStampDutyRates() {
        // Default rates are used if state-specific rates are not found
        // These can be extended with actual state-specific rates if they differ
        stateStampDutyRates.put("MH", 0.015); // Maharashtra
        stateStampDutyRates.put("KA", 0.015); // Karnataka
        stateStampDutyRates.put("TN", 0.015); // Tamil Nadu
        stateStampDutyRates.put("DL", 0.015); // Delhi
    }
    
    /**
     * Initialize broker-specific fees from configuration
     */
    private void initializeBrokerFees() {
        // Add common discount brokers
        brokerFees.put("zerodha", 20.0);
        brokerFees.put("upstox", 20.0);
        brokerFees.put("mstock", 20.0);
        brokerFees.put("angelone", 20.0);
        brokerFees.put("groww", 20.0);
        brokerFees.put("dhan", 20.0);
        brokerFees.put("fyers", 20.0);
        brokerFees.put("icicidirect", 20.0);
        brokerFees.put("5paisa", 10.0);
        brokerFees.put("aliceblue", 15.0);
        brokerFees.put("finvasia", 0.0);
        brokerFees.put("vested", 0.0);
        brokerFees.put("flattrade", 0.0);
        
        // Add full-service brokers
        brokerFees.put("hdfcsec", 0.5);
        brokerFees.put("kotaksecurities", 0.5);
        brokerFees.put("sharekhan", 0.5);
        brokerFees.put("motilaloswal", 0.5);
        
        // Log the broker fees
        if (!brokerFees.isEmpty()) {
            log.info("Loaded broker fees: {}", brokerFees.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ")));
        } else {
            log.info("No broker-specific fees configured, using defaults");
        }
    }

    /**
     * Calculate brokerage and all applicable taxes and charges for a trade
     * 
     * @param request The brokerage calculation request
     * @return BrokerageCalculationResponse with calculated charges
     */
    public BrokerageCalculationResponse calculateBrokerage(BrokerageCalculationRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("Calculating brokerage and charges for {} trade of {} shares of {}", 
                request.getTradeType(), request.getQuantity(), request.getTradingSymbol());
        
        try {
            // Calculate transaction values
            BigDecimal buyTransactionValue = request.getBuyPrice()
                    .multiply(BigDecimal.valueOf(request.getQuantity()));
            
            BigDecimal sellTransactionValue = BigDecimal.ZERO;
            if (request.getSellPrice() != null) {
                sellTransactionValue = request.getSellPrice()
                        .multiply(BigDecimal.valueOf(request.getQuantity()));
            }
            
            // Calculate brokerage
            BigDecimal buyBrokerage = calculateBrokerageFee(buyTransactionValue, request);
            BigDecimal sellBrokerage = BigDecimal.ZERO;
            if (request.getSellPrice() != null) {
                sellBrokerage = calculateBrokerageFee(sellTransactionValue, request);
            }
            
            // Calculate STT
            BigDecimal buySTT = calculateSTT(buyTransactionValue, request, true);
            BigDecimal sellSTT = BigDecimal.ZERO;
            if (request.getSellPrice() != null) {
                sellSTT = calculateSTT(sellTransactionValue, request, false);
            }
            
            // Calculate exchange charges
            BigDecimal buyExchangeCharges = calculateExchangeCharges(buyTransactionValue, request);
            BigDecimal sellExchangeCharges = BigDecimal.ZERO;
            if (request.getSellPrice() != null) {
                sellExchangeCharges = calculateExchangeCharges(sellTransactionValue, request);
            }
            
            // Calculate SEBI charges
            BigDecimal buySEBICharges = calculateSEBICharges(buyTransactionValue);
            BigDecimal sellSEBICharges = BigDecimal.ZERO;
            if (request.getSellPrice() != null) {
                sellSEBICharges = calculateSEBICharges(sellTransactionValue);
            }
            
            // Calculate stamp duty
            BigDecimal buyStampDuty = calculateStampDuty(buyTransactionValue, request, true);
            BigDecimal sellStampDuty = BigDecimal.ZERO;
            if (request.getSellPrice() != null) {
                sellStampDuty = calculateStampDuty(sellTransactionValue, request, false);
            }
            
            // Calculate GST on brokerage and other charges
            BigDecimal buyGST = calculateGST(buyBrokerage.add(buyExchangeCharges));
            BigDecimal sellGST = BigDecimal.ZERO;
            if (request.getSellPrice() != null) {
                sellGST = calculateGST(sellBrokerage.add(sellExchangeCharges));
            }
            
            // Calculate DP charges (only applicable on sell transactions)
            BigDecimal dpCharges = BigDecimal.ZERO;
            if (request.getSellPrice() != null && 
                    request.getTradeType() == BrokerageCalculationRequest.TradeType.DELIVERY) {
                dpCharges = BigDecimal.valueOf(dpCharge);
            }
            
            // Calculate total charges
            BigDecimal totalBuyCharges = buyBrokerage.add(buySTT).add(buyExchangeCharges)
                    .add(buySEBICharges).add(buyStampDuty).add(buyGST);
            
            BigDecimal totalSellCharges = sellBrokerage.add(sellSTT).add(sellExchangeCharges)
                    .add(sellSEBICharges).add(sellStampDuty).add(sellGST).add(dpCharges);
            
            BigDecimal totalCharges = totalBuyCharges.add(totalSellCharges);
            
            // Calculate net profit/loss if sell price is provided
            BigDecimal netProfitLoss = BigDecimal.ZERO;
            if (request.getSellPrice() != null) {
                netProfitLoss = sellTransactionValue.subtract(buyTransactionValue).subtract(totalCharges);
            }
            
            // Calculate charges percentage
            BigDecimal chargesPercentage = BigDecimal.ZERO;
            if (buyTransactionValue.compareTo(BigDecimal.ZERO) > 0) {
                chargesPercentage = totalCharges.multiply(BigDecimal.valueOf(100))
                        .divide(buyTransactionValue, 4, RoundingMode.HALF_UP);
            }
            
            // Calculate breakeven price
            BigDecimal breakEvenPrice = calculateBreakEvenPrice(
                    buyTransactionValue, totalBuyCharges, totalSellCharges, request.getQuantity());
            
            // Build response
            return BrokerageCalculationResponse.builder()
                    .buyTransactionValue(buyTransactionValue)
                    .sellTransactionValue(sellTransactionValue)
                    .buyBrokerage(buyBrokerage)
                    .sellBrokerage(sellBrokerage)
                    .buySTT(buySTT)
                    .sellSTT(sellSTT)
                    .buyExchangeCharges(buyExchangeCharges)
                    .sellExchangeCharges(sellExchangeCharges)
                    .buyGST(buyGST)
                    .sellGST(sellGST)
                    .buySEBICharges(buySEBICharges)
                    .sellSEBICharges(sellSEBICharges)
                    .buyStampDuty(buyStampDuty)
                    .sellStampDuty(sellStampDuty)
                    .dpCharges(dpCharges)
                    .totalBuyCharges(totalBuyCharges)
                    .totalSellCharges(totalSellCharges)
                    .totalCharges(totalCharges)
                    .netProfitLoss(netProfitLoss)
                    .chargesPercentage(chargesPercentage)
                    .breakEvenPrice(breakEvenPrice)
                    .status("SUCCESS")
                    .build();
            
        } catch (Exception e) {
            log.error("Error calculating brokerage: {}", e.getMessage(), e);
            meterRegistry.counter("market-data.brokerage.calculation.error").increment();
            
            // Return error response
            return BrokerageCalculationResponse.builder()
                    .status("ERROR")
                    .error("Failed to calculate brokerage: " + e.getMessage())
                    .build();
        } finally {
            sample.stop(meterRegistry.timer("market-data.brokerage.calculation.time"));
        }
    }
    
    /**
     * Calculate brokerage asynchronously
     * 
     * @param request The brokerage calculation request
     * @return CompletableFuture with the brokerage calculation response
     */
    public CompletableFuture<BrokerageCalculationResponse> calculateBrokerageAsync(
            BrokerageCalculationRequest request) {
        return CompletableFuture.supplyAsync(() -> calculateBrokerage(request), threadPoolExecutor);
    }
    
    /**
     * Calculate brokerage fee based on broker type, broker name, and transaction value
     * 
     * @param transactionValue The transaction value
     * @param request The brokerage calculation request
     * @return Brokerage fee
     */
    private BigDecimal calculateBrokerageFee(BigDecimal transactionValue, BrokerageCalculationRequest request) {
        // If explicit fee is provided in the request, use that
        if (request.getBrokerFlatFee() != null) {
            return request.getBrokerFlatFee();
        }
        
        if (request.getBrokerPercentageFee() != null) {
            return transactionValue.multiply(request.getBrokerPercentageFee())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        
        // Check if we have a broker-specific fee
        if (request.getBrokerName() != null && !request.getBrokerName().isEmpty()) {
            String brokerName = request.getBrokerName().toLowerCase();
            if (brokerFees.containsKey(brokerName)) {
                if (request.getBrokerType() == BrokerageCalculationRequest.BrokerType.DISCOUNT) {
                    // Discount broker - flat fee
                    return BigDecimal.valueOf(brokerFees.get(brokerName));
                } else {
                    // Full service broker - percentage fee
                    return transactionValue.multiply(BigDecimal.valueOf(brokerFees.get(brokerName)))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
            }
        }
        
        // Fall back to default fees based on broker type
        if (request.getBrokerType() == BrokerageCalculationRequest.BrokerType.DISCOUNT) {
            // Default discount broker flat fee
            return BigDecimal.valueOf(discountBrokerFlatFee);
        } else {
            // Default full service broker percentage fee
            return transactionValue.multiply(BigDecimal.valueOf(fullServiceBrokerPercentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * Calculate Securities Transaction Tax (STT)
     * 
     * @param transactionValue The transaction value
     * @param request The brokerage calculation request
     * @param isBuy Whether this is a buy transaction
     * @return STT amount
     */
    private BigDecimal calculateSTT(BigDecimal transactionValue, BrokerageCalculationRequest request, boolean isBuy) {
        // For intraday trades, STT is applicable only on sell side
        if (request.getTradeType() == BrokerageCalculationRequest.TradeType.INTRADAY && isBuy) {
            return BigDecimal.ZERO;
        }
        
        double sttRate = request.getTradeType() == BrokerageCalculationRequest.TradeType.DELIVERY ?
                sttDeliveryPercent : sttIntradayPercent;
        
        return transactionValue.multiply(BigDecimal.valueOf(sttRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate exchange transaction charges
     * 
     * @param transactionValue The transaction value
     * @param request The brokerage calculation request
     * @return Exchange charges
     */
    private BigDecimal calculateExchangeCharges(BigDecimal transactionValue, BrokerageCalculationRequest request) {
        double exchangeRate = "NSE".equalsIgnoreCase(request.getExchange()) ?
                nseExchangeChargePercent : bseExchangeChargePercent;
        
        return transactionValue.multiply(BigDecimal.valueOf(exchangeRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate SEBI turnover charges
     * 
     * @param transactionValue The transaction value
     * @return SEBI charges
     */
    private BigDecimal calculateSEBICharges(BigDecimal transactionValue) {
        return transactionValue.multiply(BigDecimal.valueOf(sebiChargePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate GST on applicable charges
     * 
     * @param applicableCharges The charges on which GST is applicable
     * @return GST amount
     */
    private BigDecimal calculateGST(BigDecimal applicableCharges) {
        return applicableCharges.multiply(BigDecimal.valueOf(gstPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate stamp duty
     * 
     * @param transactionValue The transaction value
     * @param request The brokerage calculation request
     * @param isBuy Whether this is a buy transaction
     * @return Stamp duty amount
     */
    private BigDecimal calculateStampDuty(BigDecimal transactionValue, 
            BrokerageCalculationRequest request, boolean isBuy) {
        // Stamp duty is applicable only on buy side
        if (!isBuy) {
            return BigDecimal.ZERO;
        }
        
        // Get state-specific rate or use default
        double stampDutyRate;
        if (request.getStateCode() != null && stateStampDutyRates.containsKey(request.getStateCode())) {
            stampDutyRate = stateStampDutyRates.get(request.getStateCode());
        } else {
            stampDutyRate = request.getTradeType() == BrokerageCalculationRequest.TradeType.DELIVERY ?
                    stampDutyDeliveryPercent : stampDutyIntradayPercent;
        }
        
        return transactionValue.multiply(BigDecimal.valueOf(stampDutyRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate breakeven price (sell price needed to cover all charges)
     * 
     * @param buyValue Buy transaction value
     * @param buyCharges Total buy charges
     * @param sellChargesPerUnit Estimated sell charges per unit
     * @param quantity Number of shares
     * @return Breakeven price
     */
    private BigDecimal calculateBreakEvenPrice(BigDecimal buyValue, BigDecimal buyCharges,
            BigDecimal sellChargesPerUnit, int quantity) {
        
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate buy price per unit including charges
        BigDecimal buyPriceWithCharges = buyValue.add(buyCharges)
                .divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        
        // Estimate sell charges per unit
        BigDecimal sellChargesEstimate = sellChargesPerUnit.divide(
                BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        
        // Breakeven price = buy price with charges + estimated sell charges per unit
        return buyPriceWithCharges.add(sellChargesEstimate);
    }
}
