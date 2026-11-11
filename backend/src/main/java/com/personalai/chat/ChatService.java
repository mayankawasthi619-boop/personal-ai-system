package com.personalai.chat;

import com.personalai.conversation.ConversationService;
import com.personalai.memory.MemoryService;
import com.personalai.model.ModelRouterService;
import com.personalai.rag.RagService;
import com.personalai.tool.ToolRegistry;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationService conversationService;
    private final MemoryService memoryService;
    private final RagService ragService;
    private final ToolRegistry toolRegistry;
    private final ModelRouterService modelRouterService;

    @Value("${app.ai.prompt-path:./ai/prompts}")
    private String promptPath;

    @Data
    @Builder
    public static class ChatRequest {
        private UUID conversationId;
        private String message;
        private String model;
        private boolean stream;
    }

    public UUID resolveOrCreateConversation(UUID userId, UUID requestedId, String userMessage) {
        if (requestedId != null) {
            return requestedId;
        }
        String titleSnippet = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
        var conv = conversationService.createConversation(userId, titleSnippet);
        return conv.getId();
    }

    public Flux<String> processStreamChat(UUID userId, UUID conversationId, ChatRequest request) {
        log.info("Processing stream chat for user [{}] in conversation [{}]", userId, conversationId);

        // 1. Persist User Message
        conversationService.addMessage(conversationId, "user", request.getMessage(), request.getModel(), null);

        // 2. Retrieve multi-turn conversation history (last 10 messages before current turn)
        List<ConversationService.MessageDto> history = Collections.emptyList();
        try {
            history = conversationService.getConversationMessages(userId, conversationId);
        } catch (Exception ex) {
            log.warn("Could not load message history: {}", ex.getMessage());
        }

        StringBuilder historyBuilder = new StringBuilder();
        if (history.size() > 1) {
            int startIndex = Math.max(0, history.size() - 11);
            for (int i = startIndex; i < history.size() - 1; i++) {
                var m = history.get(i);
                historyBuilder.append(m.getRole().equalsIgnoreCase("user") ? "User: " : "Assistant: ")
                        .append(m.getContent())
                        .append("\n\n");
            }
        }
        String conversationHistory = historyBuilder.toString().trim();

        // 3. Retrieve relevant long-term memories
        List<MemoryService.MemoryDto> memories = Collections.emptyList();
        try {
            memories = memoryService.retrieveRelevantMemories(userId, request.getMessage(), 5);
        } catch (Exception ex) {
            log.warn("Memory retrieval failed: {}", ex.getMessage());
        }
        String memoryContext = memories.stream()
                .map(m -> "- (" + m.getType() + "): " + m.getContent())
                .collect(Collectors.joining("\n"));

        // 4. Retrieve RAG Document Context
        List<RagService.RagQueryResult> ragChunks = Collections.emptyList();
        try {
            ragChunks = ragService.retrieveRelevantContext(userId, request.getMessage(), 3);
        } catch (Exception ex) {
            log.warn("RAG retrieval failed: {}", ex.getMessage());
        }
        String ragContext = ragChunks.stream()
                .map(c -> "[Doc: " + c.getSourceDocument() + "]\n" + c.getContent())
                .collect(Collectors.joining("\n\n"));

        // 5. Intelligent Tool Pre-Execution if relevant
        String toolExecutionOutput = checkAndExecuteTools(userId, request.getMessage());

        // 6. Build Hydrated System Prompt
        String systemPromptTemplate = loadPromptTemplate("system.txt");
        String hydratedSystemPrompt = systemPromptTemplate
                .replace("{{assistant_name}}", "Personal AI")
                .replace("{{user_name}}", "User")
                .replace("{{user_languages}}", "Python, Java, TypeScript")
                .replace("{{user_tech_stack}}", "Spring Boot, React, Postgres, Docker")
                .replace("{{user_preferences}}", "Prefers concise, clean, production-grade code.")
                .replace("{{memory}}", memoryContext.isBlank() ? "No specific relevant memory." : memoryContext)
                .replace("{{documents}}", ragContext.isBlank() ? "No document context matched." : ragContext)
                .replace("{{tools}}", toolRegistry.getToolMetadataList().toString());

        // 7. Model Selection
        ChatModel chatModel = modelRouterService.resolveChatModel(request.getModel(), "GENERAL");
        String resolvedModelName = modelRouterService.resolveModelName(request.getModel(), "GENERAL");

        // 8. Construct Prompt and Stream
        StringBuilder fullPromptBuilder = new StringBuilder(hydratedSystemPrompt);

        if (!conversationHistory.isBlank()) {
            fullPromptBuilder.append("\n\nPRIOR CONVERSATION HISTORY:\n").append(conversationHistory);
        }

        if (!toolExecutionOutput.isBlank()) {
            fullPromptBuilder.append("\n\nVERIFIED REAL-TIME TOOL EXECUTION RESULT:\n").append(toolExecutionOutput);
        }

        fullPromptBuilder.append("\n\nUser Question:\n").append(request.getMessage());

        Prompt prompt = new Prompt(fullPromptBuilder.toString());

        StringBuilder accumulatedResponse = new StringBuilder();
        final UUID finalConvId = conversationId;
        final String userMessage = request.getMessage();

        return chatModel.stream(prompt)
                .map(chunk -> {
                    String token = chunk.getResult() != null && chunk.getResult().getOutput() != null
                            ? chunk.getResult().getOutput().getContent()
                            : "";
                    if (token != null) {
                        accumulatedResponse.append(token);
                    }
                    return token != null ? token : "";
                })
                .onErrorResume(err -> {
                    log.error("Streaming error in chat: ", err);
                    String fallbackMessage = "\n\n⚠️ Error during AI response generation: " + err.getMessage() +
                            "\nPlease verify your LLM provider (Ollama / OpenAI) is configured.";
                    accumulatedResponse.append(fallbackMessage);
                    return Flux.just(fallbackMessage);
                })
                .doOnComplete(() -> {
                    String finalAssistantText = accumulatedResponse.toString();
                    // 9. Persist Assistant Response on completion
                    conversationService.addMessage(finalConvId, "assistant", finalAssistantText, resolvedModelName, null);
                    log.info("Chat response complete and persisted for conversation [{}]", finalConvId);

                    // 10. Asynchronously extract and persist memories
                    try {
                        memoryService.extractAndPersistMemoriesAsync(userId, userMessage, finalAssistantText);
                    } catch (Exception ex) {
                        log.warn("Async memory extraction dispatch failed: {}", ex.getMessage());
                    }
                });
    }

    private String checkAndExecuteTools(UUID userId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return "";
        String msg = userMessage.trim().toLowerCase();

        try {
            // 1. Date / Time query
            if (msg.contains("what time") || msg.contains("what is the time") ||
                    msg.contains("current time") || msg.contains("today's date") ||
                    msg.contains("current date") || msg.contains("what is the date") ||
                    msg.contains("what day is it")) {
                Map<String, Object> res = toolRegistry.executeTool(userId, "current_datetime", Map.of("timezone", "UTC"));
                return "[Tool: current_datetime]: " + res;
            }

            // 2. Unit converter
            if (msg.contains("convert ") && (msg.contains("celsius") || msg.contains("fahrenheit") || msg.contains("km") || msg.contains("miles") || msg.contains("kg") || msg.contains("lbs"))) {
                Pattern p = Pattern.compile("convert\\s+([0-9.]+)\\s+([a-zA-Z]+)\\s+to\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(userMessage);
                if (m.find()) {
                    Map<String, Object> input = Map.of(
                            "value", Double.parseDouble(m.group(1)),
                            "from", m.group(2).toLowerCase(),
                            "to", m.group(3).toLowerCase()
                    );
                    Map<String, Object> res = toolRegistry.executeTool(userId, "unit_converter", input);
                    if (!res.containsKey("error")) {
                        return "[Tool: unit_converter]: " + res;
                    }
                }
            }

            // 3. Calculator query
            if (msg.startsWith("calc ") || msg.startsWith("calculate ") || msg.matches(".*\\d+\\s*[+\\-*/^%]\\s*\\d+.*")) {
                String expr = userMessage.replaceAll("(?i)^(calc|calculate|what is)\\s*", "").replaceAll("[?]", "").trim();
                Map<String, Object> res = toolRegistry.executeTool(userId, "calculator", Map.of("expression", expr));
                if (!res.containsKey("error")) {
                    return "[Tool: calculator]: " + res;
                }
            }

            // 4. Code execution
            if (msg.startsWith("run python:") || msg.startsWith("exec python:")) {
                String code = userMessage.replaceFirst("(?i)^(run python:|exec python:)", "").trim();
                if (code.startsWith("```python")) code = code.substring(9);
                if (code.endsWith("```")) code = code.substring(0, code.length() - 3);
                Map<String, Object> res = toolRegistry.executeTool(userId, "code_executor", Map.of("language", "python", "code", code.trim()));
                return "[Tool: code_executor]: " + res;
            }
        } catch (Exception ex) {
            log.warn("Tool auto-execution check failed: {}", ex.getMessage());
        }

        return "";
    }

    private String loadPromptTemplate(String filename) {
        // 1. Try promptPath from configuration
        try {
            Path path = Paths.get(promptPath, filename).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                return Files.readString(path);
            }
        } catch (Exception ignored) {}

        // 2. Try parent relative path (when run from backend/ directory)
        try {
            Path parentPath = Paths.get("..", promptPath, filename).toAbsolutePath().normalize();
            if (Files.exists(parentPath)) {
                return Files.readString(parentPath);
            }
        } catch (Exception ignored) {}

        // 3. Try classpath resource bundled in the jar
        try (InputStream is = getClass().getResourceAsStream("/prompts/" + filename)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        log.warn("Prompt template [{}] not found on disk or classpath. Using default fallback.", filename);
        return "You are a helpful Personal AI Assistant. User Question: {{user_prompt}}";
    }
}
