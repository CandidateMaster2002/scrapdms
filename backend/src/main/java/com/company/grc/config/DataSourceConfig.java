package com.company.grc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    /**
     * Creates the primary DataSource with a normalized JDBC URL.
     * Uses Environment parameter injection (not @Value) so placeholder
     * resolution is skipped — the raw property value is read directly,
     * which lets us rewrite postgres:// → jdbc:postgresql:// before
     * Hikari or Flyway validate the URL format.
     */
    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        // Read raw value directly — try both the env-var name and the property name
        String url = env.getProperty("SPRING_DATASOURCE_URL");
        if (url == null || url.isBlank()) {
            url = env.getProperty("spring.datasource.url");
        }

        String username = env.getProperty("SPRING_DATASOURCE_USERNAME");
        if (username == null) username = env.getProperty("spring.datasource.username", "postgres");

        String password = env.getProperty("SPRING_DATASOURCE_PASSWORD");
        if (password == null) password = env.getProperty("spring.datasource.password", "");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(normalizeUrl(url));
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);
        return new HikariDataSource(config);
    }

    private String normalizeUrl(String url) {
        if (url == null || url.startsWith("jdbc:")) return url;
        if (url.startsWith("postgres://"))   return "jdbc:postgresql://" + url.substring("postgres://".length());
        if (url.startsWith("postgresql://")) return "jdbc:postgresql://" + url.substring("postgresql://".length());
        return url;
    }
}
