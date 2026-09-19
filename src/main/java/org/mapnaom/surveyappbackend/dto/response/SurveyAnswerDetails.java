package org.mapnaom.surveyappbackend.dto.response;

import org.mapnaom.surveyappbackend.entity.SurveyAnswer;

import java.time.Instant;
import java.util.UUID;

public record SurveyAnswerDetails(UUID id, UUID responseId, UUID questionId, Integer selectedLevel,
                                  boolean skipped, Instant createdAt, Instant updatedAt) {
    public static SurveyAnswerDetails from(SurveyAnswer answer) {
        return new SurveyAnswerDetails(answer.getId(), answer.getResponse().getId(), answer.getQuestion().getId(),
                answer.getSelectedLevel(), answer.isSkipped(), answer.getCreatedAt(), answer.getUpdatedAt());
    }
}
