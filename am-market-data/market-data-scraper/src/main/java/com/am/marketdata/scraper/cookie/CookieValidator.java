package com.am.marketdata.scraper.cookie;

import com.am.marketdata.scraper.exception.CookieException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates NSE cookies for various requirements including:
 * - Presence of required cookies
 * - Expiration validation
 * - JWT token validation for nseappid
 */
@Component
@Slf4j
public class CookieValidator {
    
    // Required cookies based on NSE website
    private static final String[] REQUIRED_COOKIES = {
        "AKA_A2",       // Main session cookie
        "ak_bmsc",      // Bot management cookie
        "bm_mi",        // Bot management cookie
        "bm_sv",        // Bot management cookie
        "bm_sz",        // Bot management cookie
        "nsit",         // Session cookie
        "nseappid"      // JWT token
    };
    
    // Pattern to extract expiry date from cookie string
    private static final Pattern EXPIRES_PATTERN = Pattern.compile("Expires=([^;]+)");
    
    // Pattern to extract JWT payload
    private static final Pattern JWT_PATTERN = Pattern.compile("^([^.]+)\\.([^.]+)\\.([^.]+)$");
    
    // Date format used in cookie expiration
    private static final DateTimeFormatter COOKIE_DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("EEE, dd-MMM-yyyy HH:mm:ss zzz");
    
    /**
     * Gets the array of required cookies for validation
     * @return Array of required cookie names
     */
    public String[] getRequiredCookies() {
        return REQUIRED_COOKIES;
    }
    
    /**
     * Validates all cookies from a cookie string
     * @param cookieString Complete cookie string from HTTP headers
     * @return Map with validation results for each cookie
     */
    public Map<String, ValidationResult> validateAllCookies(String cookieString) {
        if (cookieString == null || cookieString.isEmpty()) {
            throw new CookieException("Empty cookie string provided for validation");
        }
        
        Map<String, ValidationResult> results = new HashMap<>();
        Map<String, String> cookieMap = parseCookieString(cookieString);
        
        // Check for required cookies
        for (String requiredCookie : REQUIRED_COOKIES) {
            if (!cookieMap.containsKey(requiredCookie)) {
                results.put(requiredCookie, ValidationResult.missing());
            }
        }
        
        // Validate each cookie that is present
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            
            if (results.containsKey(name)) {
                continue; // Skip already marked as missing
            }
            
            try {
                // Special validation for nseappid (JWT token)
                if ("nseappid".equals(name)) {
                    results.put(name, validateJwtToken(value));
                } else {
                    // Standard validation for other cookies
                    results.put(name, validateStandardCookie(value));
                }
            } catch (Exception e) {
                log.warn("Error validating cookie {}: {}", name, e.getMessage());
                results.put(name, ValidationResult.invalid(e.getMessage()));
            }
        }
        
        return results;
    }
    
    /**
     * Checks if all required cookies are valid
     * @param cookieString Complete cookie string
     * @return true if all required cookies are valid
     */
    public boolean areRequiredCookiesValid(String cookieString) {
        Map<String, ValidationResult> results = validateAllCookies(cookieString);
        
        for (String requiredCookie : REQUIRED_COOKIES) {
            ValidationResult result = results.get(requiredCookie);
            if (result == null || !result.isValid()) {
                log.warn("Required cookie {} is invalid: {}", 
                        requiredCookie, 
                        result == null ? "not validated" : result.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if cookies are about to expire
     * @param cookieString Complete cookie string
     * @param minutesThreshold Minutes before expiration to consider as "about to expire"
     * @return true if any required cookie will expire within the threshold
     */
    public boolean areAnyRequiredCookiesExpiringSoon(String cookieString, int minutesThreshold) {
        Map<String, String> cookieMap = parseCookieString(cookieString);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        
        for (String requiredCookie : REQUIRED_COOKIES) {
            String value = cookieMap.get(requiredCookie);
            if (value == null) continue;
            
            LocalDateTime expiryDate = extractExpiryDate(value);
            if (expiryDate != null) {
                if (expiryDate.minusMinutes(minutesThreshold).isBefore(now)) {
                    log.info("Cookie {} will expire soon (at {})", requiredCookie, expiryDate);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Validates a standard (non-JWT) cookie
     */
    private ValidationResult validateStandardCookie(String value) {
        if (value == null || value.isEmpty()) {
            return ValidationResult.invalid("Empty value");
        }
        
        // Check for invalid markers
        if (value.contains("expired") || value.contains("invalid")) {
            return ValidationResult.invalid("Contains invalid markers");
        }
        
        // Check expiration if available
        LocalDateTime expiryDate = extractExpiryDate(value);
        if (expiryDate != null) {
            if (expiryDate.isBefore(LocalDateTime.now(ZoneId.of("UTC")))) {
                return ValidationResult.expired(expiryDate);
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates JWT token (nseappid cookie)
     */
    private ValidationResult validateJwtToken(String token) {
        if (token == null || token.isEmpty()) {
            return ValidationResult.invalid("Empty JWT token");
        }
        
        // Check JWT format (header.payload.signature)
        Matcher matcher = JWT_PATTERN.matcher(token);
        if (!matcher.matches()) {
            return ValidationResult.invalid("Invalid JWT format");
        }
        
        try {
            // Decode payload
            String payload = matcher.group(2);
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
            
            // Check for expiration in payload
            if (decodedPayload.contains("\"exp\":")) {
                // Extract expiration timestamp
                Pattern expPattern = Pattern.compile("\"exp\":(\\d+)");
                Matcher expMatcher = expPattern.matcher(decodedPayload);
                
                if (expMatcher.find()) {
                    long expTimestamp = Long.parseLong(expMatcher.group(1));
                    long currentTimestamp = System.currentTimeMillis() / 1000;
                    
                    if (expTimestamp < currentTimestamp) {
                        return ValidationResult.expired(
                            LocalDateTime.ofEpochSecond(expTimestamp, 0, 
                                java.time.ZoneOffset.UTC));
                    }
                }
            }
            
            return ValidationResult.valid();
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid("Invalid JWT encoding: " + e.getMessage());
        }
    }
    
    /**
     * Extracts expiry date from cookie value if present
     */
    private LocalDateTime extractExpiryDate(String cookieValue) {
        Matcher matcher = EXPIRES_PATTERN.matcher(cookieValue);
        if (matcher.find()) {
            String expiresStr = matcher.group(1);
            try {
                return LocalDateTime.parse(expiresStr, COOKIE_DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                log.debug("Could not parse expiry date: {}", expiresStr);
                return null;
            }
        }
        return null;
    }
    
    /**
     * Parses cookie string into map of name-value pairs
     */
    private Map<String, String> parseCookieString(String cookieString) {
        Map<String, String> cookieMap = new HashMap<>();
        
        if (cookieString == null || cookieString.isEmpty()) {
            return cookieMap;
        }
        
        String[] cookies = cookieString.split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2) {
                cookieMap.put(parts[0].trim(), parts[1].trim());
            }
        }
        
        return cookieMap;
    }
    
    /**
     * Represents the result of cookie validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final boolean expired;
        private final LocalDateTime expiryDate;
        
        private ValidationResult(boolean valid, String message, boolean expired, LocalDateTime expiryDate) {
            this.valid = valid;
            this.message = message;
            this.expired = expired;
            this.expiryDate = expiryDate;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, "Valid", false, null);
        }
        
        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason, false, null);
        }
        
        public static ValidationResult expired(LocalDateTime expiryDate) {
            return new ValidationResult(false, "Expired", true, expiryDate);
        }
        
        public static ValidationResult missing() {
            return new ValidationResult(false, "Missing", false, null);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public boolean isExpired() {
            return expired;
        }
        
        public String getMessage() {
            return message;
        }
        
        public LocalDateTime getExpiryDate() {
            return expiryDate;
        }
        
        @Override
        public String toString() {
            if (valid) {
                return "Valid";
            } else if (expired) {
                return "Expired at " + expiryDate;
            } else {
                return message;
            }
        }
    }
}
