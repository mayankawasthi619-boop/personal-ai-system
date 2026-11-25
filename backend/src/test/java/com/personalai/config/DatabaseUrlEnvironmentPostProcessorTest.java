package com.personalai.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void testRenderDatabaseUrlTransformation() {
        StandardEnvironment environment = new StandardEnvironment();
        // Exact Render PostgreSQL connection string format
        String renderUrl = "postgresql://personalai:JQnRbREqxvDqHpAUGGUxCwkvdfjXNT09@dpg-dc3atj49v7es738eerq0-a/personal_ai_db_0rmb";
        environment.getPropertySources().addFirst(new MapPropertySource("testEnv", Map.of("DATABASE_URL", renderUrl)));

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, null);

        // Verify JDBC transformation
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(jdbcUrl, "JDBC URL must not be null");
        assertEquals("jdbc:postgresql://dpg-dc3atj49v7es738eerq0-a:5432/personal_ai_db_0rmb", jdbcUrl);

        assertEquals("personalai", environment.getProperty("spring.datasource.username"));
        assertEquals("JQnRbREqxvDqHpAUGGUxCwkvdfjXNT09", environment.getProperty("spring.datasource.password"));

        // Verify DATABASE_URL was overridden to JDBC
        assertEquals("jdbc:postgresql://dpg-dc3atj49v7es738eerq0-a:5432/personal_ai_db_0rmb", environment.getProperty("DATABASE_URL"));
    }
}
