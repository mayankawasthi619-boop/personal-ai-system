package com.personalai.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    private boolean isCloudOpenAiAvailable() {
        return openAiChatModel.isPresent() &&
                openAiApiKey != null &&
                !openAiApiKey.isBlank() &&
                !openAiApiKey.contains("demo-key") &&
                !openAiApiKey.contains("your_openai_api_key");
    }

    public ChatModel resolveChatModel(String requestedModel, String taskType) {
        log.info("Resolving LLM model for request: [RequestedModel: {}, TaskType: {}, SystemMode: {}]",
                requestedModel, taskType, systemMode);

        // Force local if mode is strictly LOCAL
        if ("LOCAL".equalsIgnoreCase(systemMode)) {
            log.info("System forced to LOCAL mode. Using Ollama chat model.");
            return ollamaChatModel;
        }

        // If explicitly requested OpenAI or Cloud reasoning
        if ("CLOUD".equalsIgnoreCase(systemMode) || "gpt-4o".equalsIgnoreCase(requestedModel) || "gpt-4o-mini".equalsIgnoreCase(requestedModel)) {
            if (isCloudOpenAiAvailable()) {
                return openAiChatModel.get();
            }
            log.warn("Cloud OpenAI model requested but valid API key is not configured. Falling back to local Ollama.");
            return ollamaChatModel;
        }

        // HYBRID Mode: Route based on task complexity
        if ("HYBRID".equalsIgnoreCase(systemMode)) {
            if ("CODING_COMPLEX".equalsIgnoreCase(taskType) || "REASONING_HEAVY".equalsIgnoreCase(taskType)) {
                if (isCloudOpenAiAvailable()) {
                    log.info("Hybrid Router: Task [{}] requires complex reasoning. Routing to Cloud OpenAI.", taskType);
                    return openAiChatModel.get();
                }
            }
        }

        log.info("Defaulting to Local Ollama ChatModel.");
        return ollamaChatModel;
    }

    public String resolveModelName(String requestedModel, String taskType) {
        if ("LOCAL".equalsIgnoreCase(systemMode)) {
            return "ollama:llama3.2";
        }
        if (isCloudOpenAiAvailable() && ("CLOUD".equalsIgnoreCase(systemMode) || "gpt-4o".equalsIgnoreCase(requestedModel) || "gpt-4o-mini".equalsIgnoreCase(requestedModel))) {
            return "openai:gpt-4o-mini";
        }
        return "ollama:llama3.2";
    }
}
