package org.mapnaom.surveyappbackend.dto.survey;

import org.mapnaom.surveyappbackend.entity.SurveyRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SurveyDashboardResponse(Instant generatedAt, Summary summary, List<SurveyStats> surveys) {
    public record Summary(long totalSurveys, long activeSurveys, long inactiveSurveys,
                          long totalQuestions, long totalResponses, long unassignedResponses,
                          long totalAnswers, long answeredAnswers, long skippedAnswers,
                          BigDecimal skipRate) {
    }

    public record SurveyStats(Long id, String title, String version, boolean active,
                              Instant createdAt, Instant updatedAt, long questionCount,
                              long responseCount, long answerCount, long answeredCount,
                              long skippedCount, BigDecimal skipRate, Instant lastSubmittedAt,
                              List<AudienceStats> audiences) {
    }

    public record AudienceStats(SurveyRole role, long responseCount, long answerCount,
                                long answeredCount, long skippedCount, BigDecimal skipRate) {
    }

}
