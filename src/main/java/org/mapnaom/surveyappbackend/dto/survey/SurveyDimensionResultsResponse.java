package org.mapnaom.surveyappbackend.dto.survey;

import org.mapnaom.surveyappbackend.entity.SurveyRole;

import java.time.Instant;
import java.util.List;

/** Chart-compatible survey score data, including an aggregate and role groups. */
public record SurveyDimensionResultsResponse(
        Long surveyId,
        Instant generatedAt,
        List<DimensionResult> dimensions,
        List<RoleResult> roles) {

    public record RoleResult(SurveyRole role, List<DimensionResult> dimensions) {
    }

    public record DimensionResult(String id, String label, String color, double value,
                                  List<CriterionResult> criteria) {
    }

    public record CriterionResult(String id, String label, double value,
                                  List<PointResult> points) {
    }

    public record PointResult(String id, String label, double value) {
    }
}
