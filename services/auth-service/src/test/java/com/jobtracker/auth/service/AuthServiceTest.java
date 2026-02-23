package com.jobtracker.auth.service;

import com.jobtracker.auth.dto.AuthResponse;
import com.jobtracker.auth.dto.LoginRequest;
import com.jobtracker.auth.dto.RegisterRequest;
import com.jobtracker.auth.dto.UserResponse;
import com.jobtracker.auth.entity.User;
import com.jobtracker.auth.exception.ConflictException;
import com.jobtracker.auth.exception.UnauthorizedException;
import com.jobtracker.auth.repository.UserRepository;
import com.jobtracker.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    // Shared fixtures — reset implicitly between tests because Mockito creates
    // a fresh mock set for each test method via @ExtendWith(MockitoExtension.class).
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "alice@example.com";
    private static final String RAW_PASSWORD = "secret99";
    private static final String HASHED_PASSWORD = "$2a$10$hashedvalue";
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test.token";

    // Helper: build a persisted User with id already set (simulates what the DB returns).
    private User buildPersistedUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setPasswordHash(HASHED_PASSWORD);
        return user;
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    void register_success_savesUserAndReturnsTokenWithUserResponse() {
        // Arrange
        RegisterRequest req = new RegisterRequest(EMAIL, RAW_PASSWORD);

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);

        // Simulate save() returning the user with an id assigned by the DB.
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(USER_ID);
            return u;
        });

        when(jwtUtil.generateToken(USER_ID, EMAIL)).thenReturn(JWT_TOKEN);

        // Act
        AuthResponse response = authService.register(req);

        // Assert: token and user data are mapped correctly.
        assertThat(response.token()).isEqualTo(JWT_TOKEN);
        assertThat(response.user().id()).isEqualTo(USER_ID);
        assertThat(response.user().email()).isEqualTo(EMAIL);

        // Assert: password was encoded before saving.
        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        assertThat(savedUserCaptor.getValue().getPasswordHash()).isEqualTo(HASHED_PASSWORD);
        assertThat(savedUserCaptor.getValue().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void register_duplicateEmail_throwsConflictException() {
        // Arrange
        RegisterRequest req = new RegisterRequest(EMAIL, RAW_PASSWORD);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already registered");

        // The user must never be persisted when the email is already taken.
        verify(userRepository, never()).save(any(User.class));
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    void login_success_returnsTokenAndUserResponse() {
        // Arrange
        LoginRequest req = new LoginRequest(EMAIL, RAW_PASSWORD);
        User persistedUser = buildPersistedUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persistedUser));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtUtil.generateToken(USER_ID, EMAIL)).thenReturn(JWT_TOKEN);

        // Act
        AuthResponse response = authService.login(req);

        // Assert
        assertThat(response.token()).isEqualTo(JWT_TOKEN);
        assertThat(response.user().id()).isEqualTo(USER_ID);
        assertThat(response.user().email()).isEqualTo(EMAIL);
    }

    @Test
    void login_userNotFound_throwsUnauthorizedException() {
        // Arrange
        LoginRequest req = new LoginRequest(EMAIL, RAW_PASSWORD);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");

        // Password check must never run when the user doesn't exist.
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_wrongPassword_throwsUnauthorizedException() {
        // Arrange
        LoginRequest req = new LoginRequest(EMAIL, "wrongpassword");
        User persistedUser = buildPersistedUser();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persistedUser));
        when(passwordEncoder.matches("wrongpassword", HASHED_PASSWORD)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");

        // Token must never be issued when the password is wrong.
        verify(jwtUtil, never()).generateToken(any(), anyString());
    }

    // -------------------------------------------------------------------------
    // getMe
    // -------------------------------------------------------------------------

    @Test
    void getMe_success_returnsUserResponse() {
        // Arrange
        User persistedUser = buildPersistedUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(persistedUser));

        // Act
        UserResponse response = authService.getMe(USER_ID);

        // Assert
        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo(EMAIL);
    }

    @Test
    void getMe_userNotFound_throwsUnauthorizedException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.getMe(USER_ID))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired token");
    }
}
