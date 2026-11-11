package com.personalai.conversation;

import com.personalai.exception.ApiException;
import com.personalai.user.User;
import com.personalai.user.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Data
    @Builder
    @AllArgsConstructor
    public static class ConversationDto {
        private UUID id;
        private String title;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MessageDto {
        private UUID id;
        private UUID conversationId;
        private String role;
        private String content;
        private String model;
        private Integer tokenCount;
        private Map<String, Object> metadata;
        private Instant createdAt;
    }

    @Transactional
    public ConversationDto createConversation(UUID userId, String initialTitle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        String title = (initialTitle != null && !initialTitle.isBlank()) ? initialTitle : "New Conversation";

        Conversation conversation = Conversation.builder()
                .user(user)
                .title(title)
                .build();

        Conversation saved = conversationRepository.save(conversation);
        return mapToConversationDto(saved);
    }

    public Page<ConversationDto> getUserConversations(UUID userId, int page, int size) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::mapToConversationDto);
    }

    public ConversationDto getConversation(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException("Conversation not found", "CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapToConversationDto(conversation);
    }

    public List<MessageDto> getConversationMessages(UUID userId, UUID conversationId) {
        // Ensure user owns conversation
        getConversation(userId, conversationId);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::mapToMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationDto updateConversationTitle(UUID userId, UUID conversationId, String newTitle) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException("Conversation not found", "CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        conversation.setTitle(newTitle);
        Conversation updated = conversationRepository.save(conversation);
        return mapToConversationDto(updated);
    }

    @Transactional
    public void deleteConversation(UUID userId, UUID conversationId) {
        getConversation(userId, conversationId);
        conversationRepository.deleteByIdAndUserId(conversationId, userId);
    }

    @Transactional
    public Message addMessage(UUID conversationId, String role, String content, String model, Map<String, Object> metadata) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException("Conversation not found", "CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        Message message = Message.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .model(model)
                .tokenCount(estimateTokenCount(content))
                .metadata(metadata)
                .build();

        Message saved = messageRepository.save(message);

        // Update conversation timestamp
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        return saved;
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) return 0;
        return (int) Math.ceil(text.length() / 4.0);
    }

    private ConversationDto mapToConversationDto(Conversation c) {
        return ConversationDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private MessageDto mapToMessageDto(Message m) {
        return MessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .role(m.getRole())
                .content(m.getContent())
                .model(m.getModel())
                .tokenCount(m.getTokenCount())
                .metadata(m.getMetadata())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
