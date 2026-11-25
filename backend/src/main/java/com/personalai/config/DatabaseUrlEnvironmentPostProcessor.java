package com.personalai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes cloud provider database URLs (e.g. Render, Railway, Heroku)
 * which typically follow postgresql://user:pass@host:port/db format into
 * Spring Boot's required jdbc:postgresql://host:port/db format with credentials.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Pattern DB_PATTERN = Pattern.compile(
            "^(?:postgres(?:ql)?|jdbc:postgresql)://(?:([^:@]+)(?::([^@]+))?@)?([^:/?#]+)(?::(\\d+))?(/[^?#]*)?(?:\\?(.*))?$"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            return;
        }

        // Normalize postgres:// or postgresql:// into standard JDBC format
        if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
            String host = null;
            int port = 5432;
            String path = "";
            String username = null;
            String password = null;

            Matcher matcher = DB_PATTERN.matcher(dbUrl);
            if (matcher.matches()) {
                username = matcher.group(1);
                password = matcher.group(2);
                host = matcher.group(3);
                String portStr = matcher.group(4);
                if (portStr != null && !portStr.isBlank()) {
                    try {
                        port = Integer.parseInt(portStr);
                    } catch (NumberFormatException ignored) {}
                }
                path = matcher.group(5) != null ? matcher.group(5) : "";
            } else {
                try {
                    String cleanUrl = dbUrl.replaceFirst("^postgres(ql)?://", "http://");
                    URI uri = new URI(cleanUrl);
                    host = uri.getHost();
                    port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    path = uri.getPath() != null ? uri.getPath() : "";
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        password = parts[1];
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to parse DATABASE_URL: " + ex.getMessage());
                }
            }

            if (host != null && !host.isBlank()) {
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                System.out.println("Successfully transformed Cloud DATABASE_URL to JDBC: " + jdbcUrl);

                Map<String, Object> overrides = new HashMap<>();
                overrides.put("spring.datasource.url", jdbcUrl);
                overrides.put("spring.flyway.url", jdbcUrl);
                overrides.put("DATABASE_URL", jdbcUrl);
                overrides.put("SPRING_DATASOURCE_URL", jdbcUrl);

                if (username != null && !username.isBlank()) {
                    overrides.put("spring.datasource.username", username);
                    overrides.put("spring.flyway.user", username);
                    overrides.put("DATABASE_USERNAME", username);
                }
                if (password != null && !password.isBlank()) {
                    overrides.put("spring.datasource.password", password);
                    overrides.put("spring.flyway.password", password);
                    overrides.put("DATABASE_PASSWORD", password);
                }

                environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabaseUrlOverride", overrides));
            }
        }
    }
}
