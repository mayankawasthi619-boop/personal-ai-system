package com.personalai.user;

import com.personalai.common.ApiResponse;
import com.personalai.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserService.UserProfileDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserService.UserProfileDto profile = userService.getUserProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @Data
    public static class UpdateUserRequest {
        private String name;
        private Map<String, String> preferences;
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserService.UserProfileDto>> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UpdateUserRequest request) {
        UserService.UserProfileDto profile = userService.updateUserProfile(userPrincipal.getId(), request.getName(), request.getPreferences());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
