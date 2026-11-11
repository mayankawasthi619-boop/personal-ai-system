package com.personalai.tool;

import com.personalai.user.User;
import com.personalai.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolRegistry {

    private final List<Tool> toolList;
    private final ToolExecutionRepository toolExecutionRepository;
    private final UserRepository userRepository;

    private Map<String, Tool> toolMap;

    public Map<String, Tool> getTools() {
        if (toolMap == null) {
            toolMap = toolList.stream().collect(Collectors.toMap(Tool::getName, Function.identity()));
        }
        return toolMap;
    }

    public List<Map<String, Object>> getToolMetadataList() {
        return getTools().values().stream()
                .map(t -> Map.of(
                        "name", (Object) t.getName(),
                        "description", t.getDescription(),
                        "inputSchema", t.getInputSchema()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> executeTool(UUID userId, String toolName, Map<String, Object> input) {
        Tool tool = getTools().get(toolName);
        if (tool == null) {
            return Map.of("error", "Tool not found: " + toolName);
        }

        log.info("Executing tool [{}] for user [{}]", toolName, userId);

        Map<String, Object> output;
        String status = "SUCCESS";
        try {
            output = tool.execute(input);
            if (output.containsKey("error")) {
                status = "FAILED";
            }
        } catch (Exception ex) {
            log.error("Tool execution failed: {}", toolName, ex);
            output = Map.of("error", ex.getMessage());
            status = "FAILED";
        }

        // Persist Audit Log
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                ToolExecution audit = ToolExecution.builder()
                        .user(user)
                        .toolName(toolName)
                        .inputData(input)
                        .outputData(output)
                        .status(status)
                        .build();
                toolExecutionRepository.save(audit);
            }
        } catch (Exception auditEx) {
            log.warn("Failed to save tool audit log: {}", auditEx.getMessage());
        }

        return output;
    }
}
