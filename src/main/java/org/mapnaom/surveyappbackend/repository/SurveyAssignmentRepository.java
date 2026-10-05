package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.SurveyAssignment;
import org.mapnaom.surveyappbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyAssignmentRepository extends JpaRepository<SurveyAssignment, Long>,
        JpaSpecificationExecutor<SurveyAssignment> {
    Optional<SurveyAssignment> findByUserIdAndSurveyId(Long userId, Long surveyId);

    boolean existsByUserIdAndSurveyId(Long userId, Long surveyId);

    List<SurveyAssignment> findAllByUserId(Long userId);

    List<SurveyAssignment> findAllBySurveyId(Long surveyId);

    // Admin history includes all statuses and windows, including disabled/deleted users.
    @EntityGraph(attributePaths = {"user", "survey"})
    List<SurveyAssignment> findAllBySurveyIdOrderByAssignedAtDescIdDesc(Long surveyId);

    /**
     * All assignments for a user that are currently valid: survey active, status
     * ASSIGNED/ACTIVE, not revoked, and inside the optional time window.
     */
    default List<SurveyAssignment> findActiveAssignments(Long userId, Instant now) {
        return findAllByUserId(userId).stream()
                .filter(assignment -> assignment.getSurvey().isActive())
                .filter(assignment -> assignment.isCurrentlyActive(now))
                .toList();
    }

    /**
     * Assignments listed on the user's profile page: same rules as
     * {@link #findActiveAssignments} but completed assignments inside their
     * window are included so the UI can show them as completed.
     */
    default List<SurveyAssignment> findProfileAssignments(Long userId, Instant now) {
        return findAllByUserId(userId).stream()
                .filter(assignment -> assignment.getSurvey().isActive())
                .filter(assignment -> assignment.isVisibleOnProfile(now))
                .toList();
    }
}
