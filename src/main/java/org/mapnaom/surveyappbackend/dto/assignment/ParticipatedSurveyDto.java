package org.mapnaom.surveyappbackend.dto.assignment;

import lombok.Builder;
import lombok.Data;
import org.mapnaom.surveyappbackend.dto.survey.SurveyResponseDto;

import java.time.Instant;
import java.util.UUID;

/**
 * A survey the current user has participated in, derived from the user's
 * {@link org.mapnaom.surveyappbackend.entity.SurveyResponse} records.
 */
@Data
@Builder
public class ParticipatedSurveyDto {
    private SurveyResponseDto survey;
    private UUID responseId;
    private Instant submittedAt;
}
