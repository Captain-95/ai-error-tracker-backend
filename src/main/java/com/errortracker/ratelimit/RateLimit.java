package com.errortracker.ratelimit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    // Redis key prefix — use something descriptive like "auth:login" or "sdk:ingest"
    String key() default "default";

    // Max number of requests allowed in the time window
    int limit() default 60;

    // Time window in seconds
    long windowSeconds() default 60;

    // Where to get the identity from for rate limiting
    KeySource keySource() default KeySource.IP;

    enum KeySource {
        IP,           // Rate limit by client IP address
        API_KEY,      // Rate limit by X-API-KEY header (used by SDK)
        USER_EMAIL    // Rate limit by authenticated user email (from JWT)
    }
}