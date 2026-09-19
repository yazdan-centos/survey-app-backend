package org.mapnaom.surveyappbackend.dto.response;

import org.mapnaom.surveyappbackend.entity.SurveyResponse;
import org.mapnaom.surveyappbackend.entity.SurveyRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SurveyResponseDetails(UUID id, SurveyRole role, String respondentUsername, Instant submittedAt,
                                    List<SurveyAnswerDetails> answers, List<DemographicDetails> demographics,
                                    Instant createdAt, Instant updatedAt) {
    public static SurveyResponseDetails from(SurveyResponse response) {
        return new SurveyResponseDetails(response.getId(), response.getRole(), response.getRespondentUsername(),
                response.getSubmittedAt(), response.getAnswers().stream().map(SurveyAnswerDetails::from).toList(),
                response.getDemographics().stream()
                        .map(d -> new DemographicDetails(d.getId(), d.getFieldKey(), d.getValue())).toList(),
                response.getCreatedAt(), response.getUpdatedAt());
    }

    public record DemographicDetails(UUID id, String fieldKey, String value) {
    }
}
