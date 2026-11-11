package com.personalai.memory;

import com.personalai.embedding.EmbeddingService;
import com.personalai.exception.ApiException;
import com.personalai.user.User;
import com.personalai.user.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final UserRepository userRepository;
    private final EmbeddingService embeddingService;

    @Data
    @Builder
    @AllArgsConstructor
    public static class MemoryDto {
        private UUID id;
        private String type;
        private String content;
        private Float importance;
        private Float confidence;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Transactional
    public MemoryDto createMemory(UUID userId, String type, String content, Float importance, Float confidence) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        float[] embedding = embeddingService.getEmbedding(content);

        Memory memory = Memory.builder()
                .user(user)
                .type(type != null ? type : "SEMANTIC")
                .content(content)
                .importance(importance != null ? importance : 0.5f)
                .confidence(confidence != null ? confidence : 1.0f)
                .embedding(embedding != null ? new com.pgvector.PGvector(embedding) : null)
                .build();

        Memory saved = memoryRepository.save(memory);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MemoryDto> getUserMemories(UUID userId) {
        return memoryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemoryDto> retrieveRelevantMemories(UUID userId, String queryText, int limit) {
        float[] queryEmbedding = embeddingService.getEmbedding(queryText);
        String embeddingString = embeddingService.vectorToString(queryEmbedding);

        return memoryRepository.findRelevantMemories(userId, embeddingString, limit).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @org.springframework.scheduling.annotation.Async
    @Transactional
    public void extractAndPersistMemoriesAsync(UUID userId, String userMessage, String assistantResponse) {
        if (userMessage == null || userMessage.isBlank()) return;

        String lower = userMessage.toLowerCase().trim();

        // Detect explicit or implicit preference/profile statements
        String type = null;
        String content = null;
        float importance = 0.5f;

        if (lower.startsWith("i prefer") || lower.startsWith("my preference") || lower.contains("always use ") || lower.contains("don't use ")) {
            type = "PROCEDURAL";
            content = userMessage.trim();
            importance = 0.8f;
        } else if (lower.startsWith("i am ") || lower.startsWith("my name is") || lower.startsWith("i work ") || lower.contains("i live in") || lower.startsWith("remember that")) {
            type = "SEMANTIC";
            content = userMessage.replaceFirst("(?i)^remember that\\s*", "").trim();
            importance = 0.85f;
        } else if (lower.startsWith("my goal is") || lower.startsWith("i want to achieve") || lower.startsWith("task:") || lower.startsWith("todo:")) {
            type = "TASK";
            content = userMessage.trim();
            importance = 0.75f;
        }

        if (type != null && content != null && !content.isBlank()) {
            final String finalContent = content;
            boolean alreadyExists = memoryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .anyMatch(m -> m.getContent().equalsIgnoreCase(finalContent));

            if (!alreadyExists) {
                createMemory(userId, type, finalContent, importance, 0.9f);
            }
        }
    }

    @Transactional
    public void deleteMemory(UUID userId, UUID memoryId) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new ApiException("Memory not found", "MEMORY_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!memory.getUser().getId().equals(userId)) {
            throw new ApiException("Forbidden", "FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        memoryRepository.delete(memory);
    }

    @Transactional
    public void clearAllMemories(UUID userId) {
        memoryRepository.deleteAllByUserId(userId);
    }

    private MemoryDto mapToDto(Memory m) {
        return MemoryDto.builder()
                .id(m.getId())
                .type(m.getType())
                .content(m.getContent())
                .importance(m.getImportance())
                .confidence(m.getConfidence())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
