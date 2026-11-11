package com.personalai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes cloud provider database URLs (e.g. Render, Railway, Heroku)
 * which typically follow postgres://user:pass@host:port/db format into
 * Spring Boot's required jdbc:postgresql://host:port/db format with credentials.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("DATABASE_URL");
        if (dbUrl != null && (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://"))) {
            try {
                String cleanUrl = dbUrl.replaceFirst("^postgres(ql)?://", "http://");
                URI uri = new URI(cleanUrl);

                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;

                Map<String, Object> overrides = new HashMap<>();
                overrides.put("spring.datasource.url", jdbcUrl);

                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    overrides.put("spring.datasource.username", parts[0]);
                    overrides.put("spring.datasource.password", parts[1]);
                }

                environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabaseUrlOverride", overrides));
            } catch (Exception ignored) {
                // If parsing fails, allow Spring Boot's standard property resolution to proceed
            }
        }
    }
}
