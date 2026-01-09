package com.am.marketdata.config;

import com.am.common.investment.persistence.repository.instrument.InstrumentRepository;
import com.am.common.investment.persistence.repository.StockIndicesMarketDataRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import java.lang.reflect.Proxy;

@Configuration
@Profile("isolated")
public class IsolatedRepoConfig {

    @Bean
    public InstrumentRepository instrumentRepository() {
        return (InstrumentRepository) Proxy.newProxyInstance(
                InstrumentRepository.class.getClassLoader(),
                new Class[] { InstrumentRepository.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockInstrumentRepository";
                    return null;
                });
    }

    @Bean
    public StockIndicesMarketDataRepository stockIndicesMarketDataRepository() {
        return (StockIndicesMarketDataRepository) Proxy.newProxyInstance(
                StockIndicesMarketDataRepository.class.getClassLoader(),
                new Class[] { StockIndicesMarketDataRepository.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockStockIndicesMarketDataRepository";
                    return null;
                });
    }

    @Bean
    public com.am.marketdata.internal.repository.MarketDataIngestionStatusRepository marketDataIngestionStatusRepository() {
        return (com.am.marketdata.internal.repository.MarketDataIngestionStatusRepository) Proxy.newProxyInstance(
                com.am.marketdata.internal.repository.MarketDataIngestionStatusRepository.class.getClassLoader(),
                new Class[] { com.am.marketdata.internal.repository.MarketDataIngestionStatusRepository.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockMarketDataIngestionStatusRepository";
                    return null;
                });
    }

    @Bean
    public com.am.marketdata.internal.repository.IngestionJobLogRepository ingestionJobLogRepository() {
        return (com.am.marketdata.internal.repository.IngestionJobLogRepository) Proxy.newProxyInstance(
                com.am.marketdata.internal.repository.IngestionJobLogRepository.class.getClassLoader(),
                new Class[] { com.am.marketdata.internal.repository.IngestionJobLogRepository.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockIngestionJobLogRepository";
                    return null;
                });
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return (RedisConnectionFactory) Proxy.newProxyInstance(
                RedisConnectionFactory.class.getClassLoader(),
                new Class[] { RedisConnectionFactory.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockRedisConnectionFactory";

                    if (method.getName().equals("getConnection")) {
                        return (RedisConnection) Proxy.newProxyInstance(
                                RedisConnection.class.getClassLoader(),
                                new Class[] { RedisConnection.class },
                                (connProxy, connMethod, connArgs) -> {
                                    // Prevent NPE on connection object methods too
                                    if (connMethod.getName().equals("close"))
                                        return null;
                                    if (connMethod.getName().equals("isClosed"))
                                        return false;
                                    return null; // Return null for other methods
                                });
                    }
                    return null;
                });
    }

    @Bean
    public org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new org.springframework.data.redis.core.StringRedisTemplate(factory);
    }

    @Bean
    public java.util.concurrent.ThreadPoolExecutor threadPoolExecutor() {
        return new java.util.concurrent.ThreadPoolExecutor(
                1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>());
    }

    @Bean
    public org.springframework.kafka.core.ProducerFactory<String, Object> producerFactory() {
        return (org.springframework.kafka.core.ProducerFactory<String, Object>) Proxy.newProxyInstance(
                org.springframework.kafka.core.ProducerFactory.class.getClassLoader(),
                new Class[] { org.springframework.kafka.core.ProducerFactory.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockProducerFactory";
                    if (method.getName().equals("transactionCapable"))
                        return false;
                    return null;
                });
    }

    @Bean
    public org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate(
            org.springframework.kafka.core.ProducerFactory<String, Object> producerFactory) {
        return new org.springframework.kafka.core.KafkaTemplate<>(producerFactory);
    }

    @Bean
    public org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate(
            org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory) {
        org.springframework.data.redis.core.RedisTemplate<String, Object> template = new org.springframework.data.redis.core.RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public com.am.marketdata.service.repo.SecurityRepository securityRepository() {
        return (com.am.marketdata.service.repo.SecurityRepository) Proxy.newProxyInstance(
                com.am.marketdata.service.repo.SecurityRepository.class.getClassLoader(),
                new Class[] { com.am.marketdata.service.repo.SecurityRepository.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockSecurityRepository";
                    return null;
                });
    }

    @Bean
    public com.am.marketdata.service.repo.UpstoxInstrumentRepository upstoxInstrumentRepository() {
        return (com.am.marketdata.service.repo.UpstoxInstrumentRepository) Proxy.newProxyInstance(
                com.am.marketdata.service.repo.UpstoxInstrumentRepository.class.getClassLoader(),
                new Class[] { com.am.marketdata.service.repo.UpstoxInstrumentRepository.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockUpstoxInstrumentRepository";
                    return null;
                });
    }

    @Bean
    public com.am.marketdata.service.repo.ZerodhaInstrumentRepository zerodhaInstrumentRepository() {
        return (com.am.marketdata.service.repo.ZerodhaInstrumentRepository) Proxy.newProxyInstance(
                com.am.marketdata.service.repo.ZerodhaInstrumentRepository.class.getClassLoader(),
                new Class[] { com.am.marketdata.service.repo.ZerodhaInstrumentRepository.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockZerodhaInstrumentRepository";
                    return null;
                });
    }

    @Bean
    public org.springframework.data.mongodb.MongoDatabaseFactory mongoDatabaseFactory() {
        return (org.springframework.data.mongodb.MongoDatabaseFactory) Proxy.newProxyInstance(
                org.springframework.data.mongodb.MongoDatabaseFactory.class.getClassLoader(),
                new Class[] { org.springframework.data.mongodb.MongoDatabaseFactory.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockMongoDatabaseFactory";
                    if (method.getName().equals("getExceptionTranslator")) {
                        return new org.springframework.dao.support.PersistenceExceptionTranslator() {
                            @Override
                            public org.springframework.dao.DataAccessException translateExceptionIfPossible(
                                    RuntimeException ex) {
                                return null;
                            }
                        };
                    }
                    return null;
                });
    }

    @Bean
    public org.springframework.data.mongodb.core.MongoTemplate mongoTemplate(
            org.springframework.data.mongodb.MongoDatabaseFactory factory) {
        return new org.springframework.data.mongodb.core.MongoTemplate(factory);
    }

    @Bean
    public com.am.marketdata.kafka.config.KafkaProperties kafkaProperties() {
        com.am.marketdata.kafka.config.KafkaProperties properties = new com.am.marketdata.kafka.config.KafkaProperties();
        com.am.marketdata.kafka.config.KafkaProperties.TopicProperties topics = new com.am.marketdata.kafka.config.KafkaProperties.TopicProperties();
        topics.setStockPrice("stock-price");
        topics.setStockIndices("stock-indices");
        topics.setNseIndices("nse-indices");
        properties.setTopics(topics);
        return properties;
    }

    @Bean
    public com.am.marketdata.kafka.producer.BaseKafkaProducer<com.am.common.investment.model.events.EquityPriceUpdateEvent> equityProducer(
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        return new com.am.marketdata.kafka.producer.BaseKafkaProducer<>(kafkaTemplate);
    }

    @Bean
    public com.am.marketdata.kafka.producer.BaseKafkaProducer<com.am.common.investment.model.events.StockIndicesPriceUpdateEvent> stockIndicesProducer(
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        return new com.am.marketdata.kafka.producer.BaseKafkaProducer<>(kafkaTemplate);
    }

    @Bean
    public com.am.marketdata.provider.upstox.config.UpstoxConfig upstoxConfig() {
        return new com.am.marketdata.provider.upstox.config.UpstoxConfig();
    }

    @Bean("zerodhaMarketDataProvider")
    public com.marketdata.common.MarketDataProvider zerodhaMarketDataProvider() {
        return (com.marketdata.common.MarketDataProvider) Proxy.newProxyInstance(
                com.marketdata.common.MarketDataProvider.class.getClassLoader(),
                new Class[] { com.marketdata.common.MarketDataProvider.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockZerodhaMarketDataProvider";
                    return null;
                });
    }

    @Bean("upstoxMarketDataProvider")
    public com.marketdata.common.MarketDataProvider upstoxMarketDataProvider() {
        return (com.marketdata.common.MarketDataProvider) Proxy.newProxyInstance(
                com.marketdata.common.MarketDataProvider.class.getClassLoader(),
                new Class[] { com.marketdata.common.MarketDataProvider.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("equals"))
                        return false;
                    if (method.getName().equals("hashCode"))
                        return 0;
                    if (method.getName().equals("toString"))
                        return "MockUpstoxMarketDataProvider";
                    return null;
                });
    }

    @Bean
    public com.am.marketdata.kafka.producer.BaseKafkaProducer<com.am.common.investment.model.events.MarketIndexIndicesPriceUpdateEvent> indicesProducer(
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        return new com.am.marketdata.kafka.producer.BaseKafkaProducer<>(kafkaTemplate);
    }
}
