package com.company.grc.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Runs after .env is loaded and rewrites SPRING_DATASOURCE_URL to include
 * the jdbc: prefix if the server .env file omits it (e.g. postgres:// URLs).
 */
public class JdbcUrlNormalizingPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String url = env.getProperty("SPRING_DATASOURCE_URL");
        if (url == null || url.isBlank() || url.startsWith("jdbc:")) return;

        String normalized = normalize(url);
        env.getPropertySources().addFirst(
            new MapPropertySource("jdbcUrlNormalizer", Map.of("SPRING_DATASOURCE_URL", normalized))
        );
    }

    private String normalize(String url) {
        if (url.startsWith("postgres://")) {
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        if (url.startsWith("postgresql://")) {
            return "jdbc:postgresql://" + url.substring("postgresql://".length());
        }
        return url;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
