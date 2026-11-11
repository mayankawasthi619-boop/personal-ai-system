package com.personalai.config;

import com.pgvector.PGvector;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class VectorConfiguration {

    private final DataSource dataSource;

    @PostConstruct
    public void registerPgVector() {
        try (Connection connection = dataSource.getConnection()) {
            PGvector.registerTypes(connection);
            log.info("Successfully registered PGvector types with DataSource connection.");
        } catch (SQLException e) {
            log.warn("Could not register PGvector types with DataSource: {}", e.getMessage());
        }
    }
}
