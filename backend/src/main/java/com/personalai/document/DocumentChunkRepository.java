package com.personalai.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Query(value = """
        SELECT dc.*
        FROM document_chunks dc
        JOIN documents d ON dc.document_id = d.id
        WHERE d.user_id = :userId
        ORDER BY (dc.embedding <=> CAST(:queryEmbedding AS vector)) ASC
        LIMIT :limitCount
        """, nativeQuery = true)
    List<DocumentChunk> findRelevantChunks(
            @Param("userId") UUID userId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limitCount") int limitCount
    );
}
