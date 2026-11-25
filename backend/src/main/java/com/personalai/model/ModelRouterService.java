package com.personalai.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelRouterService {

    private final OllamaChatModel ollamaChatModel;
    private final Optional<OpenAiChatModel> openAiChatModel;
    private final ModelConfigRepository modelConfigRepository;

    @Value("${app.ai.mode:HYBRID}")
    private String systemMode; // LOCAL, CLOUD, HYBRID

    @Value("${spring.ai.openai.api-key:demo-key}")
    private String openAiApiKey;

    @Value("${spring.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${spring.ai.groq.api-key:}")
    private String groqApiKey;

    private final ConcurrentHashMap<String, ChatModel> dynamicModelCache = new ConcurrentHashMap<>();

    public boolean isCloudOpenAiAvailable() {
        return openAiChatModel.isPresent() &&
                openAiApiKey != null &&
                !openAiApiKey.isBlank() &&
                !openAiApiKey.contains("demo-key") &&
                !openAiApiKey.contains("your_openai_api_key");
    }

    public boolean isGeminiAvailable() {
        return geminiApiKey != null &&
                !geminiApiKey.isBlank() &&
                !geminiApiKey.contains("demo-key") &&
                !geminiApiKey.contains("your_gemini_api_key");
    }

    public boolean isGroqAvailable() {
        return groqApiKey != null &&
                !groqApiKey.isBlank() &&
                !groqApiKey.contains("demo-key") &&
                !groqApiKey.contains("your_groq_api_key");
    }

    public boolean hasAnyCloudProvider() {
        return isCloudOpenAiAvailable() || isGeminiAvailable() || isGroqAvailable();
    }

    public ChatModel resolveChatModel(String requestedModel, String taskType) {
        log.info("Resolving LLM model for request: [RequestedModel: {}, TaskType: {}, SystemMode: {}]",
                requestedModel, taskType, systemMode);

        String req = requestedModel != null ? requestedModel.trim().toLowerCase() : "auto";

        // 1. Explicit Gemini Request
        if (req.contains("gemini") && isGeminiAvailable()) {
            return getOrCreateGeminiModel("gemini-1.5-flash");
        }

        // 2. Explicit Groq Request
        if (req.contains("groq") && isGroqAvailable()) {
            return getOrCreateGroqModel("llama-3.3-70b-versatile");
        }

        // 3. Explicit OpenAI Request
        if ((req.contains("gpt-4") || req.contains("openai")) && isCloudOpenAiAvailable()) {
            return openAiChatModel.get();
        }

        // 4. Force local if mode is strictly LOCAL or requested ollama
        if ("LOCAL".equalsIgnoreCase(systemMode) || req.startsWith("ollama")) {
            log.info("Using Ollama local model.");
            return ollamaChatModel;
        }

        // 5. Cloud Mode: Prioritize configured cloud providers
        if ("CLOUD".equalsIgnoreCase(systemMode)) {
            if (isCloudOpenAiAvailable()) return openAiChatModel.get();
            if (isGeminiAvailable()) return getOrCreateGeminiModel("gemini-1.5-flash");
            if (isGroqAvailable()) return getOrCreateGroqModel("llama-3.3-70b-versatile");
            log.warn("Cloud mode active but no cloud API key is configured. Falling back to local Ollama.");
            return ollamaChatModel;
        }

        // 6. HYBRID Mode: Route complex tasks to Cloud if available, else fast local
        if (hasAnyCloudProvider()) {
            if ("CODING_COMPLEX".equalsIgnoreCase(taskType) || "REASONING_HEAVY".equalsIgnoreCase(taskType) || req.equals("auto")) {
                if (isCloudOpenAiAvailable()) return openAiChatModel.get();
                if (isGeminiAvailable()) return getOrCreateGeminiModel("gemini-1.5-flash");
                if (isGroqAvailable()) return getOrCreateGroqModel("llama-3.3-70b-versatile");
            }
        }

        // 7. Default to Ollama
        log.info("Defaulting to Local Ollama ChatModel.");
        return ollamaChatModel;
    }

    public String resolveModelName(String requestedModel, String taskType) {
        String req = requestedModel != null ? requestedModel.trim().toLowerCase() : "auto";
        if (req.contains("gemini") && isGeminiAvailable()) {
            return "gemini:gemini-1.5-flash";
        }
        if (req.contains("groq") && isGroqAvailable()) {
            return "groq:llama-3.3-70b-versatile";
        }
        if (isCloudOpenAiAvailable() && ("CLOUD".equalsIgnoreCase(systemMode) || req.contains("gpt-4") || req.contains("openai"))) {
            return "openai:gpt-4o-mini";
        }
        if (isGeminiAvailable() && ("CLOUD".equalsIgnoreCase(systemMode) || req.equals("auto"))) {
            return "gemini:gemini-1.5-flash";
        }
        if ("LOCAL".equalsIgnoreCase(systemMode)) {
            return "ollama:llama3.2";
        }
        return "ollama:llama3.2";
    }

    private ChatModel getOrCreateGeminiModel(String modelName) {
        return dynamicModelCache.computeIfAbsent("gemini:" + modelName, k -> {
            log.info("Creating dynamic Gemini chat model via OpenAI-compatible endpoint: {}", modelName);
            OpenAiApi api = new OpenAiApi("https://generativelanguage.googleapis.com/v1beta/openai/", geminiApiKey);
            OpenAiChatOptions options = OpenAiChatOptions.builder().withModel(modelName).withTemperature(0.7).build();
            return new OpenAiChatModel(api, options);
        });
    }

    private ChatModel getOrCreateGroqModel(String modelName) {
        return dynamicModelCache.computeIfAbsent("groq:" + modelName, k -> {
            log.info("Creating dynamic Groq chat model: {}", modelName);
            OpenAiApi api = new OpenAiApi("https://api.groq.com/openai/v1", groqApiKey);
            OpenAiChatOptions options = OpenAiChatOptions.builder().withModel(modelName).withTemperature(0.7).build();
            return new OpenAiChatModel(api, options);
        });
    }
}

