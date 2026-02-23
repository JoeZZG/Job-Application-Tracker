package com.jobtracker.application.repository;

import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.application.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByUserId(Long userId);

    List<JobApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status);

    List<JobApplication> findTop5ByUserIdAndDeadlineGreaterThanEqualOrderByDeadlineAsc(
            Long userId, LocalDate today);

    List<JobApplication> findTop5ByUserIdOrderByUpdatedAtDesc(Long userId);

    @Query("SELECT a.status, COUNT(a) FROM JobApplication a WHERE a.userId = :userId GROUP BY a.status")
    List<Object[]> countByUserIdGroupByStatus(@Param("userId") Long userId);
}
