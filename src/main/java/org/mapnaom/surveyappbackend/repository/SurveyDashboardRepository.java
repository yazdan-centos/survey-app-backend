package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.entity.SurveyRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SurveyDashboardRepository extends Repository<Survey, UUID> {
    @Query("""
            select s.id as id, s.title as title, s.version as version, s.active as active,
                   s.createdAt as createdAt, s.updatedAt as updatedAt, count(q.id) as questionCount
            from Survey s left join s.questions q
            group by s.id, s.title, s.version, s.active, s.createdAt, s.updatedAt
            order by s.createdAt desc, s.id asc
            """)
    List<SurveyOverview> findSurveyOverviews();

    @Query("""
            select q.survey.id as surveyId, r.role as role,
                   count(distinct r.id) as responseCount, count(a.id) as answerCount,
                   sum(case when a.skipped = true then 1L else 0L end) as skippedCount,
                   max(r.submittedAt) as lastSubmittedAt
            from SurveyAnswer a join a.question q join a.response r
            group by q.survey.id, r.role
            """)
    List<AudienceOverview> findAudienceOverviews();

    @Query("select count(r) from SurveyResponse r")
    long countResponses();

    @Query("select count(r) from SurveyResponse r where r.answers is empty")
    long countUnassignedResponses();

    interface SurveyOverview {
        UUID getId();
        String getTitle();
        String getVersion();
        boolean getActive();
        Instant getCreatedAt();
        Instant getUpdatedAt();
        long getQuestionCount();
    }

    interface AudienceOverview {
        UUID getSurveyId();
        SurveyRole getRole();
        long getResponseCount();
        long getAnswerCount();
        long getSkippedCount();
        Instant getLastSubmittedAt();
    }
}
