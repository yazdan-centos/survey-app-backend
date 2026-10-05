package org.mapnaom.surveyappbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Explicit join entity between {@link User} and {@link Survey} that records which
 * user a survey was assigned to, when it became available, and its lifecycle state.
 * A survey is active for a user only when the survey itself is active, the assignment
 * status is {@link SurveyAssignmentStatus#ASSIGNED} or {@link SurveyAssignmentStatus#ACTIVE},
 * it has not been revoked, and the current time falls inside the optional
 * {@code activeFrom}/{@code activeUntil} window.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "survey_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "survey_id"}))
public class SurveyAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyAssignmentStatus status = SurveyAssignmentStatus.ASSIGNED;

    @Column(nullable = false)
    private Instant assignedAt;

    @Column
    private Instant activeFrom;

    @Column
    private Instant activeUntil;

    @Column
    private Instant completedAt;

    @Column
    private Instant revokedAt;

    /**
     * Assignment window and status check used to decide whether the survey is
     * currently active for the assigned user.
     */
    public boolean isCurrentlyActive(Instant now) {
        return SurveyAssignmentStatus.isActiveStatus(status)
                && revokedAt == null
                && (activeFrom == null || !activeFrom.isAfter(now))
                && (activeUntil == null || !activeUntil.isBefore(now));
    }

    /**
     * Whether the assignment belongs on the user's profile page: like
     * {@link #isCurrentlyActive(Instant)} but a {@link SurveyAssignmentStatus#COMPLETED}
     * assignment stays listed (so it can be shown as completed) while its window is open.
     * Completed assignments are still not "currently active", so they cannot be submitted again.
     */
    public boolean isVisibleOnProfile(Instant now) {
        return (SurveyAssignmentStatus.isActiveStatus(status) || status == SurveyAssignmentStatus.COMPLETED)
                && revokedAt == null
                && (activeFrom == null || !activeFrom.isAfter(now))
                && (activeUntil == null || !activeUntil.isBefore(now));
    }
}
