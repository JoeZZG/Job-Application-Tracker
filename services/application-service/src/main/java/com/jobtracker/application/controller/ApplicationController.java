package com.jobtracker.application.controller;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.dto.DashboardSummaryResponse;
import com.jobtracker.application.dto.TargetingNoteRequest;
import com.jobtracker.application.dto.TargetingNoteResponse;
import com.jobtracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.application.service.ApplicationService;
import com.jobtracker.application.service.DashboardService;
import com.jobtracker.application.service.TargetingNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/applications")
@Tag(name = "Applications", description = "Job application tracking endpoints")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final TargetingNoteService targetingNoteService;
    private final DashboardService dashboardService;

    public ApplicationController(
            ApplicationService applicationService,
            TargetingNoteService targetingNoteService,
            DashboardService dashboardService) {
        this.applicationService = applicationService;
        this.targetingNoteService = targetingNoteService;
        this.dashboardService = dashboardService;
    }

    // -------------------------------------------------------------------------
    // Dashboard — declared before /{id} to prevent Spring treating "dashboard"
    // as a path variable.
    // -------------------------------------------------------------------------

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get dashboard summary for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getSummary(getCurrentUserId()));
    }

    // -------------------------------------------------------------------------
    // Applications CRUD
    // -------------------------------------------------------------------------

    @PostMapping
    @Operation(summary = "Create a new job application")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApplicationResponse> create(
            @Valid @RequestBody CreateApplicationRequest req) {
        ApplicationResponse response = applicationService.create(getCurrentUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all job applications, optionally filtered by status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ApplicationResponse>> list(
            @RequestParam(required = false) ApplicationStatus status) {
        return ResponseEntity.ok(applicationService.list(getCurrentUserId(), status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific job application by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not owner"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApplicationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getById(getCurrentUserId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Partially update a job application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application updated"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not owner"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApplicationResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateApplicationRequest req) {
        return ResponseEntity.ok(applicationService.update(getCurrentUserId(), id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job application")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not owner"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        applicationService.delete(getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Resume targeting notes
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/targeting-note")
    @Operation(summary = "Get the resume targeting note for an application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Targeting note returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not owner"),
            @ApiResponse(responseCode = "404", description = "Application or note not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TargetingNoteResponse> getTargetingNote(@PathVariable Long id) {
        return ResponseEntity.ok(targetingNoteService.get(getCurrentUserId(), id));
    }

    @PutMapping("/{id}/targeting-note")
    @Operation(summary = "Create or replace the resume targeting note for an application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Targeting note saved"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not owner"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TargetingNoteResponse> upsertTargetingNote(
            @PathVariable Long id,
            @RequestBody TargetingNoteRequest req) {
        return ResponseEntity.ok(targetingNoteService.upsert(getCurrentUserId(), id, req));
    }

    // -------------------------------------------------------------------------
    // Private helper
    // -------------------------------------------------------------------------

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
