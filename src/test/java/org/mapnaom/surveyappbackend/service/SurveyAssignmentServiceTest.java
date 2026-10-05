package org.mapnaom.surveyappbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapnaom.surveyappbackend.dto.assignment.ActiveSurveyDto;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.SurveyAssignment;
import org.mapnaom.surveyappbackend.entity.SurveyAssignmentStatus;
import org.mapnaom.surveyappbackend.entity.SurveyResponse;
import org.mapnaom.surveyappbackend.repository.SurveyAssignmentRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.mapnaom.surveyappbackend.repository.SurveyResponseRepository;
import org.mapnaom.surveyappbackend.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyAssignmentServiceTest {
    private static final java.util.concurrent.atomic.AtomicLong ids = new java.util.concurrent.atomic.AtomicLong(10000);
    @Mock SurveyAssignmentRepository assignmentRepository;
    @Mock UserRepository userRepository;
    @Mock SurveyRepository surveyRepository;
    @Mock SurveyResponseRepository responseRepository;
    @InjectMocks SurveyAssignmentService service;

    private static SurveyAssignment assignment(Survey survey, SurveyAssignmentStatus status,
                                               Instant from, Instant until) {
        SurveyAssignment assignment = new SurveyAssignment();
        assignment.setId(ids.incrementAndGet());
        assignment.setSurvey(survey);
        assignment.setStatus(status);
        assignment.setActiveFrom(from);
        assignment.setActiveUntil(until);
        return assignment;
    }

    private static Survey survey() {
        Survey survey = new Survey();
        survey.setId(ids.incrementAndGet());
        survey.setTitle("Survey");
        survey.setVersion(java.util.UUID.randomUUID().toString());
        survey.setActive(true);
        return survey;
    }

    @Test
    void completedAssignmentInsideWindowStaysVisibleButIsNotCurrentlyActive() {
        Instant now = Instant.now();
        SurveyAssignment completed = assignment(survey(), SurveyAssignmentStatus.COMPLETED,
                now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS));

        assertThat(completed.isVisibleOnProfile(now)).isTrue();
        // Still cannot be submitted again.
        assertThat(completed.isCurrentlyActive(now)).isFalse();
    }

    @Test
    void expiredFutureAndRevokedAssignmentsAreNotVisible() {
        Instant now = Instant.now();
        Instant past = now.minus(2, ChronoUnit.DAYS);
        Instant future = now.plus(2, ChronoUnit.DAYS);

        assertThat(assignment(survey(), SurveyAssignmentStatus.ASSIGNED, past, now.minusSeconds(1))
                .isVisibleOnProfile(now)).isFalse();
        assertThat(assignment(survey(), SurveyAssignmentStatus.COMPLETED, past, now.minusSeconds(1))
                .isVisibleOnProfile(now)).isFalse();
        assertThat(assignment(survey(), SurveyAssignmentStatus.ASSIGNED, future, null)
                .isVisibleOnProfile(now)).isFalse();
        assertThat(assignment(survey(), SurveyAssignmentStatus.REVOKED, null, null)
                .isVisibleOnProfile(now)).isFalse();
        assertThat(assignment(survey(), SurveyAssignmentStatus.EXPIRED, null, null)
                .isVisibleOnProfile(now)).isFalse();
    }

    @Test
    void profileListMarksOnlyThisUsersCompletedSurveysAndExposesResponseId() {
        Long userId = 10046L;
        Survey done = survey();
        Survey open = survey();
        SurveyAssignment completedAssignment = assignment(done, SurveyAssignmentStatus.COMPLETED, null, null);
        SurveyAssignment openAssignment = assignment(open, SurveyAssignmentStatus.ASSIGNED, null, null);
        SurveyResponse older = new SurveyResponse();
        older.setId(10047L);
        older.setSurvey(done);
        SurveyResponse newer = new SurveyResponse();
        newer.setId(10048L);
        newer.setSurvey(done);

        when(assignmentRepository.findProfileAssignments(eq(userId), any(Instant.class)))
                .thenReturn(List.of(completedAssignment, openAssignment));
        // Repository returns newest first: the first response wins.
        when(responseRepository.findAllByUserIdOrderBySubmittedAtDesc(userId))
                .thenReturn(List.of(newer, older));

        List<ActiveSurveyDto> result = service.findActiveSurveysForUser(userId);

        assertThat(result).hasSize(2);
        ActiveSurveyDto completedDto = result.get(0);
        assertThat(completedDto.isCompleted()).isTrue();
        assertThat(completedDto.getAssignmentStatus()).isEqualTo("COMPLETED");
        assertThat(completedDto.getResponseId()).isEqualTo(newer.getId());
        ActiveSurveyDto openDto = result.get(1);
        assertThat(openDto.isCompleted()).isFalse();
        assertThat(openDto.getResponseId()).isNull();
    }
}
