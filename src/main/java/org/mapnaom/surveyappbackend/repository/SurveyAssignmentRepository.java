package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.SurveyAssignment;
import org.mapnaom.surveyappbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SurveyAssignmentRepository extends JpaRepository<SurveyAssignment, UUID>,
        JpaSpecificationExecutor<SurveyAssignment> {
    Optional<SurveyAssignment> findByUserIdAndSurveyId(UUID userId, UUID surveyId);

    boolean existsByUserIdAndSurveyId(UUID userId, UUID surveyId);

    List<SurveyAssignment> findAllByUserId(UUID userId);

    List<SurveyAssignment> findAllBySurveyId(UUID surveyId);

    /**
     * All assignments for a user that are currently valid: survey active, status
     * ASSIGNED/ACTIVE, not revoked, and inside the optional time window.
     */
    default List<SurveyAssignment> findActiveAssignments(UUID userId, Instant now) {
        return findAllByUserId(userId).stream()
                .filter(assignment -> assignment.getSurvey().isActive())
                .filter(assignment -> assignment.isCurrentlyActive(now))
                .toList();
    }
}
