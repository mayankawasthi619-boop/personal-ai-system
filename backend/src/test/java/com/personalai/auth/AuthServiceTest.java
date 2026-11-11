package com.personalai.auth;

import com.personalai.exception.ApiException;
import com.personalai.security.JwtTokenProvider;
import com.personalai.user.User;
import com.personalai.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .name("Alex Turing")
                .email("alex@example.com")
                .passwordHash("hashed_password")
                .build();
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alex Turing");
        request.setEmail("alex@example.com");
        request.setPassword("securePass123");

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePass123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateAccessTokenFromUserId(sampleUser.getId())).thenReturn("mock_access_token");
        when(tokenProvider.generateRefreshToken(sampleUser.getId())).thenReturn("mock_refresh_token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertEquals("alex@example.com", response.getEmail());
        assertEquals("Alex Turing", response.getName());
    }

    @Test
    void testRegisterEmailAlreadyExistsThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alex Turing");
        request.setEmail("alex@example.com");
        request.setPassword("securePass123");

        when(userRepository.existsByEmail("alex@example.com")).thenReturn(true);

        assertThrows(ApiException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }
}
