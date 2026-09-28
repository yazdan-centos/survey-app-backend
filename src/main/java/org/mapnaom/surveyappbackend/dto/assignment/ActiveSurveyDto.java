package org.mapnaom.surveyappbackend.dto.assignment;

import lombok.Builder;
import lombok.Data;
import org.mapnaom.surveyappbackend.dto.survey.SurveyResponseDto;
import org.mapnaom.surveyappbackend.entity.SurveyAssignment;

import java.time.Instant;
import java.util.UUID;

/**
 * A survey that is currently active for a specific user, combining the survey
 * data with the assignment window and status.
 */
@Data
@Builder
public class ActiveSurveyDto {
    private SurveyResponseDto survey;
    private UUID assignmentId;
    private String assignmentStatus;
    private Instant activeFrom;
    private Instant activeUntil;

    public static ActiveSurveyDto from(SurveyAssignment assignment) {
        return ActiveSurveyDto.builder()
                .survey(SurveyResponseDto.from(assignment.getSurvey()))
                .assignmentId(assignment.getId())
                .assignmentStatus(assignment.getStatus().name())
                .activeFrom(assignment.getActiveFrom())
                .activeUntil(assignment.getActiveUntil())
                .build();
    }
}
