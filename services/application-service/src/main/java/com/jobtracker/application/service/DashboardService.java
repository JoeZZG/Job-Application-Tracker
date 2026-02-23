package com.jobtracker.application.service;

import com.jobtracker.application.config.CacheNames;
import com.jobtracker.application.dto.DashboardSummaryResponse;
import com.jobtracker.application.dto.DashboardSummaryResponse.DeadlineItem;
import com.jobtracker.application.dto.DashboardSummaryResponse.RecentItem;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.application.repository.JobApplicationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final JobApplicationRepository applicationRepository;

    public DashboardService(JobApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Cacheable(cacheNames = CacheNames.DASHBOARD_SUMMARY, key = "#userId")
    public DashboardSummaryResponse getSummary(Long userId) {
        // Total application count for this user.
        long total = applicationRepository.findByUserId(userId).size();

        // Initialise all 6 statuses to 0 so the map is always complete regardless of
        // which statuses the user actually has records for.
        Map<String, Long> byStatus = new HashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) {
            byStatus.put(s.name(), 0L);
        }

        // Fill counts from the aggregate query result.
        List<Object[]> rawCounts = applicationRepository.countByUserIdGroupByStatus(userId);
        for (Object[] row : rawCounts) {
            ApplicationStatus status = (ApplicationStatus) row[0];
            Long count = (Long) row[1];
            byStatus.put(status.name(), count);
        }

        // Next 5 upcoming deadlines on or after today.
        List<DeadlineItem> upcomingDeadlines = applicationRepository
                .findTop5ByUserIdAndDeadlineGreaterThanEqualOrderByDeadlineAsc(userId, LocalDate.now())
                .stream()
                .map(app -> new DeadlineItem(
                        app.getId(),
                        app.getCompany(),
                        app.getJobTitle(),
                        app.getDeadline() != null ? app.getDeadline().format(DateTimeFormatter.ISO_LOCAL_DATE) : null))
                .toList();

        // 5 most recently updated applications.
        List<RecentItem> recentlyUpdated = applicationRepository
                .findTop5ByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(app -> new RecentItem(
                        app.getId(),
                        app.getCompany(),
                        app.getJobTitle(),
                        app.getStatus().name(),
                        app.getUpdatedAt() != null ? app.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null))
                .toList();

        return new DashboardSummaryResponse(total, byStatus, upcomingDeadlines, recentlyUpdated);
    }
}
