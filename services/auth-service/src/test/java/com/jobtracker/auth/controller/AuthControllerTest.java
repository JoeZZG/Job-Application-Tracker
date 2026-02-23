package com.jobtracker.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.auth.dto.AuthResponse;
import com.jobtracker.auth.dto.LoginRequest;
import com.jobtracker.auth.dto.RegisterRequest;
import com.jobtracker.auth.dto.UserResponse;
import com.jobtracker.auth.exception.GlobalExceptionHandler;
import com.jobtracker.auth.security.JwtAuthenticationFilter;
import com.jobtracker.auth.security.JwtUtil;
import com.jobtracker.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * @WebMvcTest loads only the web layer (controllers, filters, ControllerAdvice).
 *
 * Security setup:
 *   - TestSecurityConfig replaces the real SecurityConfig bean, permitting all
 *     requests so that authentication does not interfere with web-layer logic tests.
 *   - JwtUtil and JwtAuthenticationFilter are @MockBean'd so Spring does not
 *     attempt to construct the real implementations (which need live @Value fields).
 *   - GlobalExceptionHandler is imported so validation error mapping is exercised.
 *
 * For GET /auth/me the production controller reads the principal from the
 * SecurityContext. We inject a pre-built Authentication via
 * SecurityMockMvcRequestPostProcessors.authentication() matching the structure
 * that JwtAuthenticationFilter sets in production (principal = Long userId).
 */
@WebMvcTest(AuthController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Mocked so the @WebMvcTest context can construct JwtAuthenticationFilter
    // without needing real @Value("${jwt.secret}") / @Value("${jwt.expiration-ms}") bindings.
    @MockBean
    private JwtUtil jwtUtil;

    // Mocked to prevent the real OncePerRequestFilter from running during tests.
    // Security rules in these tests come from TestSecurityConfig instead.
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final Long USER_ID = 42L;
    private static final String EMAIL = "bob@example.com";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.test.token";

    // -------------------------------------------------------------------------
    // POST /auth/register
    // -------------------------------------------------------------------------

    @Test
    void register_validBody_returns201WithTokenAndUser() throws Exception {
        RegisterRequest body = new RegisterRequest(EMAIL, "password1");
        AuthResponse serviceResponse = new AuthResponse(TOKEN, new UserResponse(USER_ID, EMAIL));

        when(authService.register(any(RegisterRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.user.id").value(USER_ID))
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    void register_blankEmail_returns400ValidationError() throws Exception {
        // email is blank — violates @NotBlank on RegisterRequest
        RegisterRequest body = new RegisterRequest("", "password1");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void register_shortPassword_returns400ValidationError() throws Exception {
        // password is 7 chars — violates @Size(min = 8) on RegisterRequest
        RegisterRequest body = new RegisterRequest(EMAIL, "short7!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // -------------------------------------------------------------------------
    // POST /auth/login
    // -------------------------------------------------------------------------

    @Test
    void login_validBody_returns200WithToken() throws Exception {
        LoginRequest body = new LoginRequest(EMAIL, "password1");
        AuthResponse serviceResponse = new AuthResponse(TOKEN, new UserResponse(USER_ID, EMAIL));

        when(authService.login(any(LoginRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    void login_blankEmail_returns400ValidationError() throws Exception {
        // email is blank — violates @NotBlank on LoginRequest
        LoginRequest body = new LoginRequest("", "password1");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // -------------------------------------------------------------------------
    // GET /auth/me
    // -------------------------------------------------------------------------

    @Test
    void me_authenticatedRequest_returns200WithUserResponse() throws Exception {
        UserResponse userResponse = new UserResponse(USER_ID, EMAIL);
        when(authService.getMe(USER_ID)).thenReturn(userResponse);

        /*
         * The production JwtAuthenticationFilter sets a UsernamePasswordAuthenticationToken
         * whose principal is a Long (the userId). We replicate that structure here so that
         * AuthController can cast the principal to Long without a ClassCastException.
         */
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());

        mockMvc.perform(get("/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }
}
