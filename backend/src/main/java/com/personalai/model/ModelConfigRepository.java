package com.personalai.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, UUID> {
    List<ModelConfig> findByEnabledTrue();
    Optional<ModelConfig> findByModelTypeAndEnabledTrue(String modelType);
    Optional<ModelConfig> findByName(String name);
}
