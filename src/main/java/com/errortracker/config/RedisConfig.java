package com.errortracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.url:redis://localhost:6379}")
    private String redisUrl;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            log.info("Connecting to Redis: {}",
                    redisUrl.replaceAll(":([^@]+)@", ":***@")); // hide password in logs

            // Parse the Redis URL properly
            String url = redisUrl;

            // Handle rediss:// (TLS) and redis://
            boolean useTls = url.startsWith("rediss://");

            // Remove protocol prefix
            url = url.replace("rediss://", "").replace("redis://", "");

            String host     = "localhost";
            int    port     = 6379;
            String password = null;
            int    database = 0;

            // Parse: [password@]host[:port][/db]
            if (url.contains("@")) {
                // Has password
                int atIdx = url.lastIndexOf("@");
                String userInfo = url.substring(0, atIdx);
                url = url.substring(atIdx + 1);

                // userInfo could be "user:password" or just "password"
                if (userInfo.contains(":")) {
                    password = userInfo.substring(userInfo.indexOf(":") + 1);
                } else {
                    password = userInfo;
                }
            }

            // Parse host:port/db
            if (url.contains("/")) {
                String dbStr = url.substring(url.lastIndexOf("/") + 1);
                url = url.substring(0, url.lastIndexOf("/"));
                try {
                    database = Integer.parseInt(dbStr);
                } catch (NumberFormatException ignored) {}
            }

            if (url.contains(":")) {
                host = url.substring(0, url.lastIndexOf(":"));
                port = Integer.parseInt(
                        url.substring(url.lastIndexOf(":") + 1));
            } else {
                host = url;
            }

            // Override password from property if provided separately
            if (redisPassword != null && !redisPassword.isBlank()) {
                password = redisPassword;
            }

            log.info("Redis → host: {}, port: {}, tls: {}, db: {}",
                    host, port, useTls, database);

            // Build config
            RedisStandaloneConfiguration standaloneConfig =
                    new RedisStandaloneConfiguration(host, port);
            standaloneConfig.setDatabase(database);

            if (password != null && !password.isBlank()) {
                standaloneConfig.setPassword(password);
            }

            // Build client config
            LettuceClientConfiguration.LettuceClientConfigurationBuilder
                    clientConfigBuilder = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(5));

            if (useTls) {
                clientConfigBuilder.useSsl().disablePeerVerification();
            }

            LettuceConnectionFactory factory = new LettuceConnectionFactory(
                    standaloneConfig,
                    clientConfigBuilder.build()
            );

            factory.setValidateConnection(true);
            return factory;

        } catch (Exception e) {
            log.error("Redis config failed: {} — falling back to localhost",
                    e.getMessage());
            // Fallback to localhost
            return new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration("localhost", 6379)
            );
        }
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(
            RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}