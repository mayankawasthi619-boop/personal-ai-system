package com.personalai.conversation;

import com.personalai.common.ApiResponse;
import com.personalai.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Data
    public static class CreateConversationRequest {
        private String title;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationService.ConversationDto>> createConversation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody(required = false) CreateConversationRequest request) {
        String title = request != null ? request.getTitle() : null;
        ConversationService.ConversationDto conversation = conversationService.createConversation(userPrincipal.getId(), title);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConversationService.ConversationDto>>> getUserConversations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ConversationService.ConversationDto> conversations = conversationService.getUserConversations(userPrincipal.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationService.ConversationDto>> getConversation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        ConversationService.ConversationDto conversation = conversationService.getConversation(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<ConversationService.MessageDto>>> getConversationMessages(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        List<ConversationService.MessageDto> messages = conversationService.getConversationMessages(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationService.ConversationDto>> updateTitle(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id,
            @RequestBody CreateConversationRequest request) {
        ConversationService.ConversationDto conversation = conversationService.updateConversationTitle(userPrincipal.getId(), id, request.getTitle());
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteConversation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        conversationService.deleteConversation(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully"));
    }
}
