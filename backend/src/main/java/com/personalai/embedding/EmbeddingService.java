package com.personalai.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[1536];
        }
        try {
            float[] vector = embeddingModel.embed(text);
            if (vector.length != 1536) {
                // Pad or truncate to 1536 dimensions if necessary
                return resizeVector(vector, 1536);
            }
            return vector;
        } catch (Exception ex) {
            log.warn("Embedding generation failed, returning dummy zero vector: {}", ex.getMessage());
            return new float[1536];
        }
    }

    public String vectorToString(float[] vector) {
        return Arrays.toString(vector);
    }

    private float[] resizeVector(float[] original, int newSize) {
        float[] resized = new float[newSize];
        System.arraycopy(original, 0, resized, 0, Math.min(original.length, newSize));
        return resized;
    }
}
