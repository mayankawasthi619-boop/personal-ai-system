package com.personalai.rag;

import com.personalai.document.Document;
import com.personalai.document.DocumentChunk;
import com.personalai.document.DocumentChunkRepository;
import com.personalai.embedding.EmbeddingService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    @Data
    @Builder
    @AllArgsConstructor
    public static class RagQueryResult {
        private String content;
        private String sourceDocument;
        private int chunkIndex;
    }

    @Transactional
    public void processAndStoreDocument(Document document, String fullText, int chunkSize, int chunkOverlap) {
        log.info("Chunking and vectorizing document [{}] (Length: {} chars)", document.getFilename(), fullText.length());

        List<String> textChunks = createChunks(fullText, chunkSize, chunkOverlap);
        List<DocumentChunk> entityChunks = new ArrayList<>();

        for (int i = 0; i < textChunks.size(); i++) {
            String chunkText = textChunks.get(i);
            float[] embedding = embeddingService.getEmbedding(chunkText);

            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .content(chunkText)
                    .chunkIndex(i)
                    .embedding(embedding != null ? new com.pgvector.PGvector(embedding) : null)
                    .metadata(Map.of(
                            "filename", document.getFilename(),
                            "chunkIndex", i,
                            "totalChunks", textChunks.size()
                    ))
                    .build();

            entityChunks.add(chunk);
        }

        chunkRepository.saveAll(entityChunks);
        log.info("Successfully persisted {} vector chunks for document [{}]", entityChunks.size(), document.getFilename());
    }

    @Transactional(readOnly = true)
    public List<RagQueryResult> retrieveRelevantContext(UUID userId, String query, int topK) {
        float[] queryEmbedding = embeddingService.getEmbedding(query);
        String embeddingString = embeddingService.vectorToString(queryEmbedding);

        List<DocumentChunk> chunks = chunkRepository.findRelevantChunks(userId, embeddingString, topK);

        return chunks.stream()
                .map(c -> RagQueryResult.builder()
                        .content(c.getContent())
                        .sourceDocument(c.getDocument().getFilename())
                        .chunkIndex(c.getChunkIndex())
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> createChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end).trim());
            if (end == text.length()) break;
            start += (chunkSize - overlap);
        }
        return chunks;
    }
}
