package com.am.marketdata.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("kafkaProperties")
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {

    private String bootstrapServers;
    private String groupId;
    private TopicProperties topics;
    private ProducerProperties producer;
    private Properties properties;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public TopicProperties getTopics() {
        return topics;
    }

    public void setTopics(TopicProperties topics) {
        this.topics = topics;
    }

    public ProducerProperties getProducer() {
        return producer;
    }

    public void setProducer(ProducerProperties producer) {
        this.producer = producer;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public static class TopicProperties {
        private String stockPrice;
        private String nseIndices;
        private String stockIndices;
        private String stockBoardOfDirectors;
        private String stockQuaterlyFinancials;
        private String stockBalanceSheetFinancials;
        private String stockCashFlowFinancials;
        private String stockProfitAndLossFinancials;
        private String stockResultsFinancials;
        private String stockFactSheetDividendFinancials;

        public String getStockPrice() {
            return stockPrice;
        }

        public void setStockPrice(String stockPrice) {
            this.stockPrice = stockPrice;
        }

        public String getNseIndices() {
            return nseIndices;
        }

        public void setNseIndices(String nseIndices) {
            this.nseIndices = nseIndices;
        }

        public String getStockIndices() {
            return stockIndices;
        }

        public void setStockIndices(String stockIndices) {
            this.stockIndices = stockIndices;
        }

        public String getStockBoardOfDirectors() {
            return stockBoardOfDirectors;
        }

        public void setStockBoardOfDirectors(String stockBoardOfDirectors) {
            this.stockBoardOfDirectors = stockBoardOfDirectors;
        }

        public String getStockQuaterlyFinancials() {
            return stockQuaterlyFinancials;
        }

        public void setStockQuaterlyFinancials(String stockQuaterlyFinancials) {
            this.stockQuaterlyFinancials = stockQuaterlyFinancials;
        }

        public String getStockBalanceSheetFinancials() {
            return stockBalanceSheetFinancials;
        }

        public void setStockBalanceSheetFinancials(String stockBalanceSheetFinancials) {
            this.stockBalanceSheetFinancials = stockBalanceSheetFinancials;
        }

        public String getStockCashFlowFinancials() {
            return stockCashFlowFinancials;
        }

        public void setStockCashFlowFinancials(String stockCashFlowFinancials) {
            this.stockCashFlowFinancials = stockCashFlowFinancials;
        }

        public String getStockProfitAndLossFinancials() {
            return stockProfitAndLossFinancials;
        }

        public void setStockProfitAndLossFinancials(String stockProfitAndLossFinancials) {
            this.stockProfitAndLossFinancials = stockProfitAndLossFinancials;
        }

        public String getStockResultsFinancials() {
            return stockResultsFinancials;
        }

        public void setStockResultsFinancials(String stockResultsFinancials) {
            this.stockResultsFinancials = stockResultsFinancials;
        }

        public String getStockFactSheetDividendFinancials() {
            return stockFactSheetDividendFinancials;
        }

        public void setStockFactSheetDividendFinancials(String stockFactSheetDividendFinancials) {
            this.stockFactSheetDividendFinancials = stockFactSheetDividendFinancials;
        }
    }

    public static class ProducerProperties {
        private String keySerializer;
        private String valueSerializer;
        private String acks;
        private int retries;
        private int batchSize;
        private int lingerMs;
        private int bufferMemory;

        public String getKeySerializer() {
            return keySerializer;
        }

        public void setKeySerializer(String keySerializer) {
            this.keySerializer = keySerializer;
        }

        public String getValueSerializer() {
            return valueSerializer;
        }

        public void setValueSerializer(String valueSerializer) {
            this.valueSerializer = valueSerializer;
        }

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getLingerMs() {
            return lingerMs;
        }

        public void setLingerMs(int lingerMs) {
            this.lingerMs = lingerMs;
        }

        public int getBufferMemory() {
            return bufferMemory;
        }

        public void setBufferMemory(int bufferMemory) {
            this.bufferMemory = bufferMemory;
        }
    }

    public static class Properties {
        private String securityProtocol;
        private String saslMechanism;
        private String saslJaasConfig;

        public String getSecurityProtocol() {
            return securityProtocol;
        }

        public void setSecurityProtocol(String securityProtocol) {
            this.securityProtocol = securityProtocol;
        }

        public String getSaslMechanism() {
            return saslMechanism;
        }

        public void setSaslMechanism(String saslMechanism) {
            this.saslMechanism = saslMechanism;
        }

        public String getSaslJaasConfig() {
            return saslJaasConfig;
        }

        public void setSaslJaasConfig(String saslJaasConfig) {
            this.saslJaasConfig = saslJaasConfig;
        }
    }
}
