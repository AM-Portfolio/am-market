package com.am.marketdata.api.config;

import com.am.marketdata.common.log.AppLogger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppLogger log = AppLogger.getLogger();

    @Value("${security.enabled:true}")
    private boolean securityEnabled;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        String methodName = "init";
        log.info(methodName, "=".repeat(80));
        log.info(methodName, "SECURITY CONFIGURATION INITIALIZED");
        log.info(methodName, "=".repeat(80));

        if (securityEnabled) {
            log.info(methodName, "Mode: PRODUCTION (Security ENABLED)");
            log.info(methodName, "✓ JWT Validation is ACTIVE");
            log.info(methodName, "✓ Protected endpoints: /api/v1/**");
        } else {
            log.info(methodName, "Mode: DEVELOPMENT (Security DISABLED)");
            log.warn(methodName, "⚠️  WARNING: All endpoints are PUBLIC");
        }
        log.info(methodName, "=".repeat(80));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String methodName = "filterChain";
        log.info(methodName, "Configuring Security Filter Chain...");

        http
                .cors(cors -> cors.configure(http))
                .csrf(csrf -> {
                    csrf.disable();
                    log.debug("filterChain", "CSRF protection disabled (stateless API)");
                })
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    log.debug("filterChain", "Session management: STATELESS");
                });

        if (securityEnabled) {
            log.info(methodName, "Applying PRODUCTION security configuration (JWT enabled)");

            http.authorizeHttpRequests(auth -> {
                auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/live",
                                "/actuator/health/ready",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml")
                        .permitAll()
                        .requestMatchers("/v1/**").authenticated()
                        .anyRequest().denyAll();
            })
                    .oauth2ResourceServer(oauth2 -> {
                        oauth2.jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(new com.am.marketdata.api.security.CustomJwtConverter()));
                    });

        } else {
            log.warn(methodName, "Applying DEVELOPMENT security configuration (ALL ENDPOINTS PUBLIC)");
            http.authorizeHttpRequests(auth -> {
                auth.anyRequest().permitAll();
            });
        }

        http.httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
}
