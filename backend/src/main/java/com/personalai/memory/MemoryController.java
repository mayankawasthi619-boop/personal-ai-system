package com.personalai.memory;

import com.personalai.common.ApiResponse;
import com.personalai.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @Data
    public static class CreateMemoryRequest {
        private String type;

        @NotBlank(message = "Content is required")
        private String content;

        private Float importance;
        private Float confidence;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemoryService.MemoryDto>>> getMemories(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<MemoryService.MemoryDto> memories = memoryService.getUserMemories(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(memories));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemoryService.MemoryDto>> createMemory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateMemoryRequest request) {
        MemoryService.MemoryDto memory = memoryService.createMemory(
                userPrincipal.getId(),
                request.getType(),
                request.getContent(),
                request.getImportance(),
                request.getConfidence()
        );
        return ResponseEntity.ok(ApiResponse.success(memory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMemory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        memoryService.deleteMemory(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Memory deleted successfully"));
    }

    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<String>> clearAllMemory(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        memoryService.clearAllMemories(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("All memories cleared successfully"));
    }
}
