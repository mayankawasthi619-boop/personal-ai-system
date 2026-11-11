package com.personalai.tool;

import com.personalai.common.ApiResponse;
import com.personalai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolRegistry toolRegistry;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTools() {
        List<Map<String, Object>> tools = toolRegistry.getToolMetadataList();
        return ResponseEntity.ok(ApiResponse.success(tools));
    }

    @PostMapping("/{name}/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeTool(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String name,
            @RequestBody Map<String, Object> input) {
        Map<String, Object> result = toolRegistry.executeTool(userPrincipal.getId(), name, input);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
