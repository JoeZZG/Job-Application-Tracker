package com.jobtracker.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.DashboardSummaryResponse;
import com.jobtracker.application.dto.TargetingNoteResponse;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.application.exception.GlobalExceptionHandler;
import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.application.service.DashboardService;
import com.jobtracker.application.service.TargetingNoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for ApplicationController.
 *
 * SecurityConfig and JwtAuthenticationFilter are excluded so this test does not
 * require a real JWT or a running Redis/RabbitMQ. Authentication is injected
 * directly into the SecurityContext in @BeforeEach, using a Long principal that
 * mirrors what JwtAuthenticationFilter sets at runtime.
 *
 * Assumption: the @WebMvcTest slice excludes SecurityConfig by listing only the
 * controller and GlobalExceptionHandler. Spring Security's default test behaviour
 * (requiring authenticated users) is satisfied via @WithMockUser on each test
 * combined with the SecurityContext setup below.
 */
@WebMvcTest(controllers = ApplicationController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        })
@Import(GlobalExceptionHandler.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private TargetingNoteService targetingNoteService;

    @MockBean
    private DashboardService dashboardService;

    private static final Long USER_ID = 1L;
    private static final Long APP_ID = 10L;

    // Shared ObjectMapper that can serialise LocalDate / LocalDateTime.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUpSecurityContext() {
        // Place a Long principal into the SecurityContext, mirroring what
        // JwtAuthenticationFilter does at runtime. This allows the controller's
        // getCurrentUserId() cast to succeed without a real JWT filter.
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // Convenience builder — all fields that are not the subject of a test are
    // filled with sensible defaults so assertions stay focused.
    private ApplicationResponse buildApplicationResponse(Long id) {
        return new ApplicationResponse(
                id,
                USER_ID,
                "Acme Corp",
                "Software Engineer",
                "Remote",
                "https://example.com/job/1",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 1, 15),
                ApplicationStatus.APPLIED,
                "Some notes",
                LocalDateTime.of(2026, 1, 15, 9, 0),
                LocalDateTime.of(2026, 1, 15, 9, 0)
        );
    }

    // -------------------------------------------------------------------------
    // GET /applications
    // -------------------------------------------------------------------------

    @Test
    void listApplications_authenticated_returns200() throws Exception {
        when(applicationService.list(eq(USER_ID), any())).thenReturn(
                List.of(buildApplicationResponse(APP_ID)));

        mockMvc.perform(get("/applications").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(APP_ID))
                .andExpect(jsonPath("$[0].company").value("Acme Corp"));
    }

    // -------------------------------------------------------------------------
    // POST /applications
    // -------------------------------------------------------------------------

    @Test
    void createApplication_validBody_returns201() throws Exception {
        when(applicationService.create(eq(USER_ID), any())).thenReturn(
                buildApplicationResponse(APP_ID));

        String body = """
                {
                  "company": "Acme Corp",
                  "jobTitle": "Software Engineer",
                  "location": "Remote",
                  "jobPostUrl": "https://example.com/job/1",
                  "deadline": "2026-03-01",
                  "appliedDate": "2026-01-15",
                  "status": "APPLIED",
                  "notes": "Some notes"
                }
                """;

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(APP_ID))
                .andExpect(jsonPath("$.company").value("Acme Corp"))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void createApplication_missingCompany_returns400() throws Exception {
        // company is @NotBlank on CreateApplicationRequest — omitting it must yield 400.
        String body = """
                {
                  "jobTitle": "Software Engineer"
                }
                """;

        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // -------------------------------------------------------------------------
    // GET /applications/{id}
    // -------------------------------------------------------------------------

    @Test
    void getApplication_exists_returns200() throws Exception {
        when(applicationService.getById(USER_ID, APP_ID)).thenReturn(
                buildApplicationResponse(APP_ID));

        mockMvc.perform(get("/applications/{id}", APP_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(APP_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    // -------------------------------------------------------------------------
    // PUT /applications/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateApplication_validBody_returns200() throws Exception {
        ApplicationResponse updated = new ApplicationResponse(
                APP_ID, USER_ID, "Acme Corp", "Senior Software Engineer",
                "New York", null, null, null,
                ApplicationStatus.INTERVIEW, null,
                LocalDateTime.of(2026, 1, 15, 9, 0),
                LocalDateTime.of(2026, 2, 20, 10, 0));

        when(applicationService.update(eq(USER_ID), eq(APP_ID), any())).thenReturn(updated);

        String body = """
                {
                  "jobTitle": "Senior Software Engineer",
                  "location": "New York",
                  "status": "INTERVIEW"
                }
                """;

        mockMvc.perform(put("/applications/{id}", APP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Senior Software Engineer"))
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    // -------------------------------------------------------------------------
    // DELETE /applications/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteApplication_returns204() throws Exception {
        doNothing().when(applicationService).delete(USER_ID, APP_ID);

        mockMvc.perform(delete("/applications/{id}", APP_ID))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // GET /applications/dashboard/summary
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_returns200() throws Exception {
        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                5L,
                Map.of("APPLIED", 3L, "INTERVIEW", 2L),
                List.of(),
                List.of());

        when(dashboardService.getSummary(USER_ID)).thenReturn(summary);

        mockMvc.perform(get("/applications/dashboard/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.byStatus.APPLIED").value(3));
    }
}
