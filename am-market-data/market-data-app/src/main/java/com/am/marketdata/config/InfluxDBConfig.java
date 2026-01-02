// package com.am.marketdata.config;

// import com.influxdb.client.InfluxDBClient;
// import com.influxdb.client.InfluxDBClientFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Primary;

// @Configuration
// public class InfluxDBConfig {

//     @Value("${spring.influx.url}")
//     private String url;

//     @Value("${spring.influx.token}")
//     private String token;

//     @Value("${spring.influx.org}")
//     private String org;

//     @Value("${spring.influx.bucket}")
//     private String bucket;

//     @Bean
//     @Primary
//     public InfluxDBClient influxDBClient() {
//         return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
//     }
// }
