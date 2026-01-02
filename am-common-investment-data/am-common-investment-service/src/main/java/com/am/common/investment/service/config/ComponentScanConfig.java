package com.am.common.investment.service.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "com.am.common.investment.service",
    "com.am.common.investment.persistence.repository.companyprofile",
    "com.am.common.investment.persistence.repository.measurement",
    "com.am.common.investment.persistence.repository.measurement.impl",
    "com.am.common.investment.persistence.document.companyprofile",
    "com.am.common.investment.model.board",
    "com.am.common.investment.persistence.document.measurement"
})
public class ComponentScanConfig {
}