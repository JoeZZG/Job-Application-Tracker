package com.jobtracker.notification.controller;

import com.jobtracker.notification.dto.NotificationResponse;
import com.jobtracker.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import(NotificationControllerTest.TestSecurityConfig.class)
class NotificationControllerTest {

    // ---------------------------------------------------------------------------
    // Minimal security configuration: permit all, no CSRF — mirrors what the
    // real SecurityConfig does except it skips the JWT filter entirely.
    // ---------------------------------------------------------------------------
    @Configuration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUpSecurityContext() {
        // Replicate what JwtAuthenticationFilter sets at runtime: a Long principal.
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------------
    // GET /notifications
    // ---------------------------------------------------------------------------

    @Test
    void getNotifications_authenticated_returns200WithList() throws Exception {
        NotificationResponse response = new NotificationResponse(
                10L,
                USER_ID,
                "DEADLINE_REMINDER",
                "Deadline approaching: Acme",
                "Your application for SWE at Acme has a deadline on 2026-03-01.",
                false,
                LocalDateTime.of(2026, 2, 20, 9, 0)
        );

        given(notificationService.listForUser(USER_ID)).willReturn(List.of(response));

        mockMvc.perform(get("/notifications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].userId").value(USER_ID))
                .andExpect(jsonPath("$[0].type").value("DEADLINE_REMINDER"))
                .andExpect(jsonPath("$[0].title").value("Deadline approaching: Acme"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    // ---------------------------------------------------------------------------
    // PATCH /notifications/{id}/read
    // ---------------------------------------------------------------------------

    @Test
    void markRead_authenticated_returns200() throws Exception {
        Long notificationId = 10L;
        NotificationResponse response = new NotificationResponse(
                notificationId,
                USER_ID,
                "DEADLINE_REMINDER",
                "Deadline approaching: Acme",
                "Your application for SWE at Acme has a deadline on 2026-03-01.",
                true,
                LocalDateTime.of(2026, 2, 20, 9, 0)
        );

        given(notificationService.markAsRead(USER_ID, notificationId)).willReturn(response);

        mockMvc.perform(patch("/notifications/{id}/read", notificationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.read").value(true));
    }
}
