package com.am.marketdata.service;

import com.am.marketdata.config.ISINConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ISINService {
    private final ISINConfig isinConfig;

    @PostConstruct
    public void init() {
        MDC.put("service", "isin-service");
        try {
            List<String> isins = isinConfig.list();
            if (isins == null || isins.isEmpty()) {
                log.warn("No ISINs found in configuration during initialization");
            } else {
                log.info("Loaded {} ISINs from configuration", isins.size());
                log.debug("Configured ISINs: {}", isins);
            }
        } finally {
            MDC.remove("service");
        }
    }

    public List<String> findDistinctIsins() {
        MDC.put("service", "isin-service");
        MDC.put("operation", "find-distinct");
        try {
            List<String> isins = isinConfig.list();
            if (isins == null || isins.isEmpty()) {
                log.warn("No ISINs found in configuration. Please check isin.yml configuration.");
                return Collections.emptyList();
            }
            log.info("Retrieved {} distinct ISINs", isins.size());
            log.debug("Found {} ISINs in configuration", isins.size());
            return isins;
        } finally {
            MDC.remove("service");
            MDC.remove("operation");
        }
    }
}
