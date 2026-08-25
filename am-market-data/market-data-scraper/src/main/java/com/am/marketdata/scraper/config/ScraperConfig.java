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

        // Pod runs as non-root with empty HOME; Chrome writes under HOME and crashes otherwise.
        if (System.getenv("HOME") == null || System.getenv("HOME").isBlank() || "/".equals(System.getenv("HOME"))) {
            System.setProperty("user.home", "/tmp");
        }
        System.setProperty("wdm.cachePath", "/tmp");
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        // Basic headless configuration
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-data-dir=/tmp/chrome-user-data-" + ProcessHandle.current().pid());
        options.addArguments("--disk-cache-dir=/tmp/chrome-cache");
        options.addArguments("--crash-dumps-dir=/tmp/chrome-crash");

        // Additional settings to improve reliability
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--enable-javascript");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-notifications");

        // Match installed Chrome major when possible; avoid stale hard-coded UA.
        options.addArguments(
                "--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36");

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
