package org.mapnaom.surveyappbackend.entity;

/**
 * Lifecycle of a {@link SurveyAssignment} linking a {@link User} to a {@link Survey}.
 *
 * <ul>
 *   <li>{@link #ASSIGNED} – the survey is assigned and available to the user once the window opens</li>
 *   <li>{@link #ACTIVE} – the user has opened/started the assigned survey</li>
 *   <li>{@link #COMPLETED} – the user submitted a response for the survey</li>
 *   <li>{@link #REVOKED} – the assignment was withdrawn from the user</li>
 *   <li>{@link #EXPIRED} – the assignment window has passed</li>
 * </ul>
 */
public enum SurveyAssignmentStatus {
    ASSIGNED,
    ACTIVE,
    COMPLETED,
    REVOKED,
    EXPIRED;

    /**
     * Statuses under which an assigned survey counts as active for a user.
     */
    public static boolean isActiveStatus(SurveyAssignmentStatus status) {
        return status == ASSIGNED || status == ACTIVE;
    }
}
