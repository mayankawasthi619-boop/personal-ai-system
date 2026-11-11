package com.personalai.user;

import com.personalai.exception.ApiException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @Data
    @Builder
    public static class UserProfileDto {
        private UUID id;
        private String email;
        private String name;
        private Map<String, String> preferences;
    }

    public UserProfileDto getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        Map<String, String> preferences = userPreferenceRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserPreference::getKey, UserPreference::getValue, (v1, v2) -> v2));

        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .preferences(preferences)
                .build();
    }

    @Transactional
    public UserProfileDto updateUserProfile(UUID userId, String name, Map<String, String> preferences) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (name != null && !name.isBlank()) {
            user.setName(name);
            userRepository.save(user);
        }

        if (preferences != null) {
            preferences.forEach((key, value) -> {
                UserPreference pref = userPreferenceRepository.findByUserIdAndKey(userId, key)
                        .orElseGet(() -> UserPreference.builder().user(user).key(key).build());
                pref.setValue(value);
                userPreferenceRepository.save(pref);
            });
        }

        return getUserProfile(userId);
    }
}
