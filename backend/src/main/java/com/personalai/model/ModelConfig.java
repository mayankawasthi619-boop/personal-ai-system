package com.personalai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "model_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String provider; // "ollama", "openai", "anthropic", "gemini"

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName; // "llama3.2", "gpt-4o-mini", "claude-3-5-sonnet"

    @Column(name = "model_type", nullable = false, length = 50)
    private String modelType; // "LOCAL_FAST", "LOCAL_REASONING", "CLOUD_GENERAL", "CLOUD_CODING", "VISION"

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> configuration;
}
