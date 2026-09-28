package org.mapnaom.surveyappbackend.dto.assignment;

import lombok.Builder;
import lombok.Data;
import org.mapnaom.surveyappbackend.entity.SurveyAssignment;
import org.mapnaom.surveyappbackend.entity.SurveyAssignmentStatus;
import org.mapnaom.surveyappbackend.dto.survey.SurveyResponseDto;
import org.mapnaom.surveyappbackend.dto.user.UserResponse;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SurveyAssignmentResponseDto {
    private UUID id;
    private UserResponse user;
    private SurveyResponseDto survey;
    private SurveyAssignmentStatus status;
    private Instant assignedAt;
    private Instant activeFrom;
    private Instant activeUntil;
    private Instant completedAt;
    private Instant revokedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static SurveyAssignmentResponseDto from(SurveyAssignment assignment) {
        return SurveyAssignmentResponseDto.builder()
                .id(assignment.getId())
                .user(UserResponse.from(assignment.getUser()))
                .survey(SurveyResponseDto.from(assignment.getSurvey()))
                .status(assignment.getStatus())
                .assignedAt(assignment.getAssignedAt())
                .activeFrom(assignment.getActiveFrom())
                .activeUntil(assignment.getActiveUntil())
                .completedAt(assignment.getCompletedAt())
                .revokedAt(assignment.getRevokedAt())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }
}
