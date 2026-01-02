package com.am.common.investment.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Configuration
@ComponentScan(basePackages = {
    "com.am.common.investment.service",
    "com.am.common.investment.persistence",
    "com.am.common.investment.persistence.document",
    "com.am.common.investment.persistence.repository",
    "com.am.common.investment.model.events.mapper",
    "com.am.common.investment.app",
    "com.am.common.investment.model",
    "com.am.common.investment.app.service",
    "com.am.common.investment.app.config",
    "com.am.common.investment.service.mapper"
})
@EnableMongoRepositories(basePackages = "com.am.common.investment.persistence.repository")
@EnableMongoAuditing
@Testcontainers
public class MongoTestConfig {
    
    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4")
        .withExposedPorts(27017);

    static {
        mongoDBContainer.start();
        System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());
    }

    @org.junit.jupiter.api.AfterEach
    public void afterEach() {
        mongoDBContainer.stop();
    }
}
