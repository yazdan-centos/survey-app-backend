package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDimensionResultsResponse;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDimensionResultsResponse.CriterionResult;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDimensionResultsResponse.DimensionResult;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDimensionResultsResponse.PointResult;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDimensionResultsResponse.RoleResult;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.repository.SurveyAnswerRepository;
import org.mapnaom.surveyappbackend.repository.SurveyAnswerRepository.DimensionScoreRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class SurveyDimensionResultsService {
    private static final Map<String, String> COLORS = Map.of(
            "leadership", "#0052CC",
            "strategy", "#DC2626",
            "customers", "#16A34A",
            "measurement", "#1F2937");

    private final SurveyAnswerRepository repository;

    @Transactional(readOnly = true)
    public SurveyDimensionResultsResponse getResults(Long surveyId, SurveyRole requestedRole) {
        List<DimensionScoreRow> rows = surveyId == null
                ? repository.findDimensionScoreRows()
                : repository.findDimensionScoreRowsBySurveyId(surveyId);
        if (requestedRole != null) {
            rows = rows.stream().filter(row -> row.getRole() == requestedRole).toList();
        }

        List<DimensionResult> aggregate = buildDimensions(rows);
        List<RoleResult> roles = new ArrayList<>();
        for (SurveyRole role : SurveyRole.values()) {
            List<DimensionScoreRow> roleRows = rows.stream()
                    .filter(row -> row.getRole() == role)
                    .toList();
            roles.add(new RoleResult(role, buildDimensions(roleRows)));
        }
        return new SurveyDimensionResultsResponse(surveyId, Instant.now(), aggregate, List.copyOf(roles));
    }

    private List<DimensionResult> buildDimensions(List<DimensionScoreRow> rows) {
        Map<Long, DimensionAccumulator> dimensions = new LinkedHashMap<>();
        rows.stream().sorted(Comparator.comparingInt(DimensionScoreRow::getDimensionOrder))
                .forEach(row -> dimensions.computeIfAbsent(row.getDimensionId(), ignored ->
                        new DimensionAccumulator(row)).add(row));
        return dimensions.values().stream().map(DimensionAccumulator::toResult).toList();
    }

    private static double rounded(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static final class DimensionAccumulator {
        private final String key;
        private final String label;
        private double total;
        private long count;
        private final Map<Long, CriterionAccumulator> criteria = new LinkedHashMap<>();

        private DimensionAccumulator(DimensionScoreRow row) {
            key = row.getDimensionKey();
            label = row.getDimensionLabel();
        }

        private void add(DimensionScoreRow row) {
            total += row.getScore();
            count++;
            criteria.computeIfAbsent(row.getCriterionId(), ignored -> new CriterionAccumulator(row)).add(row);
        }

        private DimensionResult toResult() {
            return new DimensionResult(key, label, COLORS.getOrDefault(key, "#64748B"),
                    rounded(total / count), criteria.values().stream()
                    .sorted(Comparator.comparing(CriterionAccumulator::label))
                    .map(CriterionAccumulator::toResult).toList());
        }
    }

    private static final class CriterionAccumulator {
        private final Long id;
        private final String label;
        private double total;
        private long count;
        private final Map<Long, PointAccumulator> points = new LinkedHashMap<>();

        private CriterionAccumulator(DimensionScoreRow row) {
            id = row.getCriterionId();
            label = row.getCriterionName();
        }

        private void add(DimensionScoreRow row) {
            total += row.getScore();
            count++;
            points.computeIfAbsent(row.getQuestionId(), ignored -> new PointAccumulator(row)).add(row);
        }

        private String label() { return label; }

        private CriterionResult toResult() {
            return new CriterionResult(id.toString(), label, rounded(total / count), points.values().stream()
                    .sorted(Comparator.comparing(PointAccumulator::label))
                    .map(PointAccumulator::toResult).toList());
        }
    }

    private static final class PointAccumulator {
        private final Long id;
        private final String label;
        private double total;
        private long count;

        private PointAccumulator(DimensionScoreRow row) {
            id = row.getQuestionId();
            label = row.getQuestionCode();
        }

        private void add(DimensionScoreRow row) {
            total += row.getScore();
            count++;
        }

        private String label() { return label; }

        private PointResult toResult() {
            return new PointResult(id.toString(), label, rounded(total / count));
        }
    }
}
