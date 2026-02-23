package com.jobtracker.application.repository;

import com.jobtracker.application.entity.ResumeTargetingNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeTargetingNoteRepository extends JpaRepository<ResumeTargetingNote, Long> {

    Optional<ResumeTargetingNote> findByApplicationId(Long applicationId);
}
