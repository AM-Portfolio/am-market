package com.am.marketdata.kafka.config;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.marketdata.common.model.events.BalanceSheetFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.BoardOfDirectorsUpdateEvent;
import com.am.marketdata.common.model.events.CashFlowFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.FactSheetFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.QuaterlyFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.StockProfitAndLossFinancialsUpdateEvent;
import com.am.marketdata.common.model.events.StockResultsFinancialsUpdateEvent;
import com.am.common.investment.model.events.EquityPriceUpdateEvent;
import com.am.common.investment.model.events.MarketIndexIndicesPriceUpdateEvent;
import com.am.common.investment.model.events.StockIndicesPriceUpdateEvent;
import com.am.marketdata.kafka.producer.BaseKafkaProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
@Profile("!isolated")
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        return new KafkaAdmin(configs);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public org.springframework.kafka.support.converter.StringJsonMessageConverter jsonMessageConverter() {
        return new org.springframework.kafka.support.converter.StringJsonMessageConverter(objectMapper());
    }

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        KafkaProperties.ConsumerProperties consumerProps = kafkaProperties.getConsumer();
        if (consumerProps != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerProps.getGroupId());
            String autoOffsetReset = consumerProps.getAutoOffsetReset();
            if (autoOffsetReset != null) {
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
            }
        } else {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "market-data-service-group");
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        }

        // Security props
        if (kafkaProperties.getProperties() != null && kafkaProperties.getProperties().getSaslJaasConfig() != null
                && !kafkaProperties.getProperties().getSaslJaasConfig().isEmpty()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                    kafkaProperties.getProperties().getSecurityProtocol());
            props.put(SaslConfigs.SASL_MECHANISM, kafkaProperties.getProperties().getSaslMechanism());
            props.put(SaslConfigs.SASL_JAAS_CONFIG, kafkaProperties.getProperties().getSaslJaasConfig());
        }

        return props;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setRecordMessageConverter(jsonMessageConverter());
        return factory;
    }

    @Bean
    public Map<String, Object> kafkaConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        if (kafkaProperties.getProperties() != null && kafkaProperties.getProperties().getSaslJaasConfig() != null
                && !kafkaProperties.getProperties().getSaslJaasConfig().isEmpty()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                    kafkaProperties.getProperties().getSecurityProtocol());
            props.put(SaslConfigs.SASL_MECHANISM, kafkaProperties.getProperties().getSaslMechanism());
            props.put(SaslConfigs.SASL_JAAS_CONFIG, kafkaProperties.getProperties().getSaslJaasConfig());
        }

        return props;
    }

    /**
     * Generic producer factory that can handle any type of object
     * 
     * @return ProducerFactory for any object type
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = kafkaConfigs();

        // Use producer properties if available, otherwise use defaults
        KafkaProperties.ProducerProperties producerProps = kafkaProperties.getProducer();
        if (producerProps != null) {
            configProps.put(ProducerConfig.ACKS_CONFIG, producerProps.getAcks());
            configProps.put(ProducerConfig.RETRIES_CONFIG, producerProps.getRetries());
            configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, producerProps.getBatchSize());
            configProps.put(ProducerConfig.LINGER_MS_CONFIG, producerProps.getLingerMs());
            configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, producerProps.getBufferMemory());
        } else {
            // Default values
            configProps.put(ProducerConfig.ACKS_CONFIG, "all");
            configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
            configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
            configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
            configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        }

        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(objectMapper());
        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), jsonSerializer);
    }

    /**
     * Generic KafkaTemplate that can be used for any type of object
     * 
     * @return KafkaTemplate for any object type
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public BaseKafkaProducer<EquityPriceUpdateEvent> equityProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<StockIndicesPriceUpdateEvent> stockIndicesProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<MarketIndexIndicesPriceUpdateEvent> indicesProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<BoardOfDirectorsUpdateEvent> boardOfDirectorsProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<QuaterlyFinancialsUpdateEvent> quaterlyFinancialsProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<BalanceSheetFinancialsUpdateEvent> balanceSheetFinancialsProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<CashFlowFinancialsUpdateEvent> cashFlowFinancialsProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<StockProfitAndLossFinancialsUpdateEvent> profitAndLossFinancialsProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<StockResultsFinancialsUpdateEvent> resultsFinancialsProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<FactSheetFinancialsUpdateEvent> factSheetFinancialsProducer() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }

    @Bean
    public BaseKafkaProducer<BoardOfDirectors> boardOfDirectors() {
        return new BaseKafkaProducer<>(kafkaTemplate());
    }
}
