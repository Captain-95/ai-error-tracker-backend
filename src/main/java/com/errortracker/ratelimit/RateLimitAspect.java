package com.errortracker.ratelimit;

import com.errortracker.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint,
                                 RateLimit rateLimit) throws Throwable {

        String identity = resolveIdentity(rateLimit);
        String redisKey = rateLimit.key() + ":" + identity;

        boolean allowed = rateLimitService.isAllowed(
                redisKey,
                rateLimit.limit(),
                rateLimit.windowSeconds()
        );

        if (!allowed) {
            throw new RateLimitException(
                    "Rate limit exceeded. Max " + rateLimit.limit() +
                            " requests per " + rateLimit.windowSeconds() + " seconds."
            );
        }

        return joinPoint.proceed();
    }

    private String resolveIdentity(RateLimit rateLimit) {
        HttpServletRequest request = getCurrentRequest();

        return switch (rateLimit.keySource()) {

            case IP -> getClientIp(request);

            case API_KEY -> {
                String apiKey = request != null
                        ? request.getHeader("X-API-KEY")
                        : null;
                yield apiKey != null ? apiKey : "unknown-api-key";
            }

            case USER_EMAIL -> {
                Authentication auth = SecurityContextHolder
                        .getContext()
                        .getAuthentication();
                yield auth != null ? auth.getName() : "anonymous";
            }
        };
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown-ip";

        // Check for IP behind a proxy or load balancer
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}