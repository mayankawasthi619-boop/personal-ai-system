package com.personalai.task;

import com.personalai.exception.ApiException;
import com.personalai.user.User;
import com.personalai.user.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Data
    @Builder
    @AllArgsConstructor
    public static class TaskDto {
        private UUID id;
        private String title;
        private String description;
        private String status;
        private String priority;
        private Instant createdAt;
        private Instant completedAt;
    }

    @Transactional
    public TaskDto createTask(UUID userId, String title, String description, String priority) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        Task task = Task.builder()
                .user(user)
                .title(title)
                .description(description)
                .priority(priority != null ? priority : "MEDIUM")
                .status("PENDING")
                .build();

        Task saved = taskRepository.save(task);
        return mapToDto(saved);
    }

    public List<TaskDto> getUserTasks(UUID userId) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDto updateTask(UUID userId, UUID taskId, String status, String priority, String title, String description) {
        Task task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ApiException("Task not found", "TASK_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (title != null && !title.isBlank()) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (priority != null) task.setPriority(priority);

        if (status != null) {
            task.setStatus(status);
            if ("COMPLETED".equalsIgnoreCase(status)) {
                task.setCompletedAt(Instant.now());
            }
        }

        Task updated = taskRepository.save(task);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteTask(UUID userId, UUID taskId) {
        taskRepository.deleteByIdAndUserId(taskId, userId);
    }

    private TaskDto mapToDto(Task t) {
        return TaskDto.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
