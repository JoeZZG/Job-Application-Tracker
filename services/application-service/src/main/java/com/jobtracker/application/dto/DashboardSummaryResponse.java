package com.jobtracker.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
        long total,
        Map<String, Long> byStatus,
        List<DeadlineItem> upcomingDeadlines,
        List<RecentItem> recentlyUpdated
) {

    public record DeadlineItem(
            Long id,
            String company,
            String jobTitle,
            LocalDate deadline
    ) {}

    public record RecentItem(
            Long id,
            String company,
            String jobTitle,
            String status,
            LocalDateTime updatedAt
    ) {}
}
