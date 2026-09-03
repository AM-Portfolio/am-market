package com.am.marketdata.service.aspect;

import com.am.marketdata.common.annotation.SmartRecommendation;
import com.am.marketdata.service.SecurityService;
import com.am.marketdata.service.model.security.SecurityDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Spring AOP Aspect that intercepts methods annotated with @SmartRecommendation.
 * 
 * <p>Centralized Responsibilities:
 * <ul>
 *   <li>Inspects method parameters for 'smartRecommendations' boolean flag.</li>
 *   <li>If false or omitted: proceeds with original legacy execution untouched.</li>
 *   <li>If true: executes sanitized, Redis-backed, market-cap-ranked recommendations.</li>
 * </ul>
 */
@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartRecommendationAspect {

    private final SecurityService securityService;

    @Around("@annotation(recommendationAnnotation)")
    public Object handleSmartRecommendation(
            ProceedingJoinPoint joinPoint,
            SmartRecommendation recommendationAnnotation) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        String query = null;
        boolean smartRequested = false;
        String category = recommendationAnnotation.category();
        int limit = recommendationAnnotation.limit();

        // Extract query and smartRecommendations flag from arguments dynamically
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                String paramName = parameterNames[i];
                Object val = args[i];

                if ("query".equalsIgnoreCase(paramName) && val instanceof String strVal) {
                    query = strVal;
                } else if ("smartRecommendations".equalsIgnoreCase(paramName)) {
                    if (val instanceof Boolean boolVal) {
                        smartRequested = boolVal;
                    }
                } else if ("category".equalsIgnoreCase(paramName) && val instanceof String catVal && !catVal.isBlank()) {
                    category = catVal;
                } else if ("limit".equalsIgnoreCase(paramName) && val instanceof Integer intVal && intVal > 0) {
                    limit = intVal;
                }
            }
        }

        // 1. Backward Compatibility Guard: If client did not opt-in to smart recommendations, run legacy code.
        if (!smartRequested) {
            return joinPoint.proceed();
        }

        // 2. Opt-in Fast-Path: Execute high-speed Redis-backed smart recommendations
        log.info("Executing @SmartRecommendation for query='{}', category='{}', limit={}", query, category, limit);
        List<SecurityDocument> recommendations = securityService.smartRecommend(query, category, limit);

        // Adapt return type if controller returns ResponseEntity<List<SecurityDocument>>
        Method method = signature.getMethod();
        if (ResponseEntity.class.isAssignableFrom(method.getReturnType())) {
            return ResponseEntity.ok(recommendations);
        }

        return recommendations;
    }
}
