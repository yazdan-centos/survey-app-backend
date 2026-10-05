package org.mapnaom.surveyappbackend.dto.assignment;

import lombok.Builder;
import lombok.Data;
import org.mapnaom.surveyappbackend.dto.survey.SurveyResponseDto;
import org.mapnaom.surveyappbackend.entity.SurveyAssignment;
import org.mapnaom.surveyappbackend.entity.SurveyAssignmentStatus;

import java.time.Instant;

/**
 * A survey that is currently active for a specific user, combining the survey
 * data with the assignment window and status.
 */
@Data
@Builder
public class ActiveSurveyDto {
    private SurveyResponseDto survey;
    private Long assignmentId;
    private String assignmentStatus;
    private Instant activeFrom;
    private Instant activeUntil;
    /** True only when this user has submitted the survey (assignment status COMPLETED). */
    private boolean completed;
    /** The user's latest response for this survey; null until they submit one. */
    private Long responseId;

    public static ActiveSurveyDto from(SurveyAssignment assignment) {
        return from(assignment, null);
    }

    public static ActiveSurveyDto from(SurveyAssignment assignment, Long responseId) {
        return ActiveSurveyDto.builder()
                .survey(SurveyResponseDto.from(assignment.getSurvey()))
                .assignmentId(assignment.getId())
                .assignmentStatus(assignment.getStatus().name())
                .activeFrom(assignment.getActiveFrom())
                .activeUntil(assignment.getActiveUntil())
                .completed(assignment.getStatus() == SurveyAssignmentStatus.COMPLETED)
                .responseId(responseId)
                .build();
    }
}
