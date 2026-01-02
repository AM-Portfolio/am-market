package com.am.marketdata.scraper.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import lombok.Data;
import jakarta.annotation.PreDestroy;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "scraper")
@Data
public class ScraperConfig {
    private List<String> urls;
    private ChromeDriver webDriver;

    @Bean
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public ChromeDriver webDriver() {
        if (webDriver != null && isSessionValid(webDriver)) {
            return webDriver;
        }

        // Clean up old driver if it exists
        if (webDriver != null) {
            try {
                webDriver.quit();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        
        // Basic headless configuration
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        // Additional settings to improve reliability
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");
        options.addArguments("--enable-javascript");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-notifications");
        
        // Set user agent to look more like a real browser
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        webDriver = new ChromeDriver(options);
        return webDriver;
    }

    private boolean isSessionValid(ChromeDriver driver) {
        try {
            // Try to get the current URL as a simple session check
            driver.getCurrentUrl();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void cleanUp() {
        if (webDriver != null) {
            try {
                webDriver.quit();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}
