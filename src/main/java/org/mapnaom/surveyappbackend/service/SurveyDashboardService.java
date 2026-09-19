package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDashboardResponse;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDashboardResponse.AudienceStats;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDashboardResponse.Summary;
import org.mapnaom.surveyappbackend.dto.survey.SurveyDashboardResponse.SurveyStats;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.mapnaom.surveyappbackend.repository.SurveyDashboardRepository;
import org.mapnaom.surveyappbackend.repository.SurveyDashboardRepository.AudienceOverview;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurveyDashboardService {
    private final SurveyDashboardRepository repository;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SurveyDashboardResponse getDashboard() {
        Instant generatedAt = Instant.now();
        var overviews = repository.findSurveyOverviews();
        Map<UUID, Map<SurveyRole, AudienceOverview>> grouped = new HashMap<>();
        for (var audience : repository.findAudienceOverviews()) {
            grouped.computeIfAbsent(audience.getSurveyId(), id -> new EnumMap<>(SurveyRole.class))
                    .put(audience.getRole(), audience);
        }
        var surveys = new ArrayList<SurveyStats>();
        for (var survey : overviews) {
            var audiences = new ArrayList<AudienceStats>();
            Map<SurveyRole, AudienceOverview> surveyAudiences = grouped.getOrDefault(survey.getId(), Map.of());
            Instant lastSubmittedAt = null;
            long responses = 0;
            long answers = 0;
            long skipped = 0;
            for (SurveyRole role : SurveyRole.values()) {
                var overview = surveyAudiences.get(role);
                long roleResponses = overview == null ? 0 : overview.getResponseCount();
                long roleAnswers = overview == null ? 0 : overview.getAnswerCount();
                long roleSkipped = overview == null ? 0 : overview.getSkippedCount();
                audiences.add(new AudienceStats(role, roleResponses, roleAnswers,
                        roleAnswers - roleSkipped, roleSkipped, skipRate(roleSkipped, roleAnswers)));
                responses += roleResponses;
                answers += roleAnswers;
                skipped += roleSkipped;
                if (overview != null && overview.getLastSubmittedAt() != null
                        && (lastSubmittedAt == null || overview.getLastSubmittedAt().isAfter(lastSubmittedAt))) {
                    lastSubmittedAt = overview.getLastSubmittedAt();
                }
            }
            surveys.add(new SurveyStats(survey.getId(), survey.getTitle(), survey.getVersion(), survey.getActive(),
                    survey.getCreatedAt(), survey.getUpdatedAt(), survey.getQuestionCount(), responses,
                    answers, answers - skipped, skipped, skipRate(skipped, answers), lastSubmittedAt,
                    List.copyOf(audiences)));
        }
        long active = surveys.stream().filter(SurveyStats::active).count();
        long questions = surveys.stream().mapToLong(SurveyStats::questionCount).sum();
        long answers = surveys.stream().mapToLong(SurveyStats::answerCount).sum();
        long skipped = surveys.stream().mapToLong(SurveyStats::skippedCount).sum();
        var summary = new Summary(surveys.size(), active, surveys.size() - active, questions,
                repository.countResponses(), repository.countUnassignedResponses(),
                answers, answers - skipped, skipped, skipRate(skipped, answers));
        return new SurveyDashboardResponse(generatedAt, summary, List.copyOf(surveys));
    }

    private BigDecimal skipRate(long skipped, long answers) {
        return answers == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(skipped).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(answers), 2, RoundingMode.HALF_UP);
    }

}
