package com.personalai.knowledge;

import com.personalai.common.ApiResponse;
import com.personalai.rag.RagService;
import com.personalai.security.UserPrincipal;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final RagService ragService;

    @Data
    public static class KnowledgeSearchRequest {
        @NotBlank(message = "Query string is required")
        private String query;
        private int topK = 5;
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<RagService.RagQueryResult>>> searchKnowledge(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody KnowledgeSearchRequest request) {
        int limit = request.getTopK() > 0 ? request.getTopK() : 5;
        List<RagService.RagQueryResult> results = ragService.retrieveRelevantContext(userPrincipal.getId(), request.getQuery(), limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
