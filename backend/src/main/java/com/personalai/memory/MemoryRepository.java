package com.personalai.memory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    List<Memory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query(value = """
        SELECT *, (1 - (embedding <=> CAST(:queryEmbedding AS vector))) AS similarity
        FROM memories
        WHERE user_id = :userId
        ORDER BY (
            0.45 * (1 - (embedding <=> CAST(:queryEmbedding AS vector))) +
            0.25 * importance +
            0.20 * EXP(-0.0001 * EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - created_at)))
        ) DESC
        LIMIT :limitCount
        """, nativeQuery = true)
    List<Memory> findRelevantMemories(
            @Param("userId") UUID userId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limitCount") int limitCount
    );

    @Modifying
    @Query("DELETE FROM Memory m WHERE m.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
