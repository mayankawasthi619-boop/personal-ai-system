package com.personalai.chat;

import com.personalai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> streamChat(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ChatService.ChatRequest request) {
        UUID conversationId = chatService.resolveOrCreateConversation(userPrincipal.getId(), request.getConversationId(), request.getMessage());
        Flux<String> stream = chatService.processStreamChat(userPrincipal.getId(), conversationId, request);

        return ResponseEntity.ok()
                .header("X-Conversation-Id", conversationId.toString())
                .body(stream);
    }
}
