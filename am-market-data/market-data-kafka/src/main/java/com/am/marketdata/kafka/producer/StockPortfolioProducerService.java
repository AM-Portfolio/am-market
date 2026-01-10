package com.am.marketdata.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.equity.financial.balancesheet.StockBalanceSheet;
import com.am.common.investment.model.equity.financial.cashflow.StockCashFlow;
import com.am.common.investment.model.equity.financial.factsheetdividend.StockFactSheetDividend;
import com.am.common.investment.model.equity.financial.profitandloss.StockProfitAndLoss;
import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
import com.am.common.investment.model.equity.financial.resultstatement.StockFinancialResult;
import com.am.marketdata.common.model.events.BalanceSheetFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.BoardOfDirectorsUpdateEvent;
import com.am.marketdata.common.model.events.CashFlowFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.FactSheetFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.QuaterlyFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.StockProfitAndLossFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.StockResultsFinancialsUpdateEvent;
import com.am.marketdata.kafka.config.KafkaProperties;

import lombok.RequiredArgsConstructor;

/**
 * Service for producing stock portfolio events to Kafka
 * Uses generic BaseKafkaProducer instances for each event type
 */
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class StockPortfolioProducerService {

    private static final Logger log = LoggerFactory.getLogger(StockPortfolioProducerService.class);

    private final BaseKafkaProducer<BoardOfDirectorsUpdateEvent> boardOfDirectorsProducer;
    private final BaseKafkaProducer<QuaterlyFinancialsUpdateEvent> quaterlyFinancialsProducer;
    private final BaseKafkaProducer<FactSheetFinancialsUpdateEvent> factSheetFinancialsProducer;
    private final BaseKafkaProducer<CashFlowFinancialsUpdateEvent> cashFlowFinancialsProducer;
    private final BaseKafkaProducer<BalanceSheetFinancialsUpdateEvent> balanceSheetFinancialsProducer;
    private final BaseKafkaProducer<StockProfitAndLossFinancialsUpdateEvent> profitAndLossFinancialsProducer;
    private final BaseKafkaProducer<StockResultsFinancialsUpdateEvent> stockResultsFinancialsProducer;

    private final KafkaProperties kafkaProperties;

    public void sendBoardOfDirectorsUpdate(String symbol, BoardOfDirectors boardOfDirectors) {
        var event = BoardOfDirectorsUpdateEvent.builder()
                .eventType("BOARD_OF_DIRECTORS_UPDATE")
                .timestamp(LocalDateTime.now())
                .symbol(symbol)
                .boardOfDirector(boardOfDirectors)
                .build();

        try {
            boardOfDirectorsProducer.send(kafkaProperties.getTopics().getStockBoardOfDirectors(), event);
            log.info("Board of directors update event sent successfully to topic: {}",
                    kafkaProperties.getTopics().getStockBoardOfDirectors());
        } catch (Exception e) {
            log.error("Failed to send board of directors update event to Kafka", e);
            throw e;
        }
    }

    public void sendQuaterlyFinancialsUpdate(String symbol, QuaterlyResult quaterlyResult) {
        var event = QuaterlyFinancialsUpdateEvent.builder()
                .eventType("QUATERLY_FINANCIALS_UPDATE")
                .timestamp(LocalDateTime.now())
                .symbol(symbol)
                .quaterlyResult(quaterlyResult)
                .build();

        try {
            quaterlyFinancialsProducer.send(kafkaProperties.getTopics().getStockQuaterlyFinancials(), event);
            log.info("Quaterly financials update event sent successfully to topic: {}",
                    kafkaProperties.getTopics().getStockQuaterlyFinancials());
        } catch (Exception e) {
            log.error("Failed to send quaterly financials update event to Kafka", e);
            throw e;
        }
    }

    public void sendBalanceSheetFinancialsUpdate(String symbol, StockBalanceSheet balanceSheet) {
        var event = BalanceSheetFinancialsUpdateEvent.builder()
                .eventType("BALANCE_SHEET_FINANCIALS_UPDATE")
                .timestamp(LocalDateTime.now())
                .symbol(symbol)
                .balanceSheet(balanceSheet)
                .build();

        try {
            balanceSheetFinancialsProducer.send(kafkaProperties.getTopics().getStockBalanceSheetFinancials(), event);
            log.info("Balance sheet financials update event sent successfully to topic: {}",
                    kafkaProperties.getTopics().getStockBalanceSheetFinancials());
        } catch (Exception e) {
            log.error("Failed to send balance sheet financials update event to Kafka", e);
            throw e;
        }
    }

    public void sendFactSheetFinancialsUpdate(String symbol, StockFactSheetDividend factSheetDividend) {
        var event = FactSheetFinancialsUpdateEvent.builder()
                .eventType("FACT_SHEET_FINANCIALS_UPDATE")
                .timestamp(LocalDateTime.now())
                .symbol(symbol)
                .factSheetDividend(factSheetDividend)
                .build();

        try {
            factSheetFinancialsProducer.send(kafkaProperties.getTopics().getStockFactSheetDividendFinancials(), event);
            log.info("Fact sheet financials update event sent successfully to topic: {}",
                    kafkaProperties.getTopics().getStockFactSheetDividendFinancials());
        } catch (Exception e) {
            log.error("Failed to send fact sheet financials update event to Kafka", e);
            throw e;
        }
    }

    public void sendCashFlowFinancialsUpdate(String symbol, StockCashFlow cashFlow) {
        var event = CashFlowFinancialsUpdateEvent.builder()
                .eventType("CASH_FLOW_FINANCIALS_UPDATE")
                .timestamp(LocalDateTime.now())
                .symbol(symbol)
                .cashFlow(cashFlow)
                .build();

        try {
            cashFlowFinancialsProducer.send(kafkaProperties.getTopics().getStockCashFlowFinancials(), event);
            log.info("Cash flow financials update event sent successfully to topic: {}",
                    kafkaProperties.getTopics().getStockCashFlowFinancials());
        } catch (Exception e) {
            log.error("Failed to send cash flow financials update event to Kafka", e);
            throw e;
        }
    }

    public void sendStockProfitAndLossFinancialsUpdate(String symbol, StockProfitAndLoss profitAndLoss) {
        var event = StockProfitAndLossFinancialsUpdateEvent.builder()
                .eventType("STOCK_PROFIT_AND_LOSS_FINANCIALS_UPDATE")
                .timestamp(LocalDateTime.now())
                .symbol(symbol)
                .profitAndLoss(profitAndLoss)
                .build();

        try {
            profitAndLossFinancialsProducer.send(kafkaProperties.getTopics().getStockProfitAndLossFinancials(), event);
            log.info("Profit and loss financials update event sent successfully to topic: {}",
                    kafkaProperties.getTopics().getStockProfitAndLossFinancials());
        } catch (Exception e) {
            log.error("Failed to send profit and loss financials update event to Kafka", e);
            throw e;
        }
    }

    public void sendStockResultsFinancialsUpdate(String symbol, StockFinancialResult results) {
        var event = StockResultsFinancialsUpdateEvent.builder()
                .eventType("STOCK_RESULTS_FINANCIALS_UPDATE")
                .timestamp(LocalDateTime.now())
                .symbol(symbol)
                .financialsReport(results)
                .build();

        try {
            stockResultsFinancialsProducer.send(kafkaProperties.getTopics().getStockResultsFinancials(), event);
            log.info("Stock results financials update event sent successfully to topic: {}",
                    kafkaProperties.getTopics().getStockResultsFinancials());
        } catch (Exception e) {
            log.error("Failed to send stock results financials update event to Kafka", e);
            throw e;
        }
    }
}
