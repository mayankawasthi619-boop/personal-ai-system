package com.personalai.task;

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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Data
    public static class CreateTaskRequest {
        @NotBlank(message = "Title is required")
        private String title;
        private String description;
        private String priority;
    }

    @Data
    public static class UpdateTaskRequest {
        private String title;
        private String description;
        private String status;
        private String priority;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskService.TaskDto>>> getTasks(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<TaskService.TaskDto> tasks = taskService.getUserTasks(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskService.TaskDto>> createTask(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskService.TaskDto task = taskService.createTask(userPrincipal.getId(), request.getTitle(), request.getDescription(), request.getPriority());
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskService.TaskDto>> updateTask(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id,
            @RequestBody UpdateTaskRequest request) {
        TaskService.TaskDto task = taskService.updateTask(userPrincipal.getId(), id, request.getStatus(), request.getPriority(), request.getTitle(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTask(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        taskService.deleteTask(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully"));
    }
}
