package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {
    Optional<SurveyAnswer> findByIdAndResponseId(Long id, Long responseId);

    @Query("""
            select d.id as dimensionId, d.key as dimensionKey, d.label as dimensionLabel,
                   d.displayOrder as dimensionOrder, c.id as criterionId, c.name as criterionName,
                   q.id as questionId, q.code as questionCode, r.role as role,
                   ql.score as score
            from SurveyAnswer a
            join a.response r
            join a.question q
            join q.criterion c
            join c.dimension d
            join q.levels ql on ql.levelNumber = a.selectedLevel
            where a.skipped = false and a.selectedLevel is not null
            order by d.displayOrder, c.name, q.displayOrder, r.role
            """)
    List<DimensionScoreRow> findDimensionScoreRows();

    @Query("""
            select d.id as dimensionId, d.key as dimensionKey, d.label as dimensionLabel,
                   d.displayOrder as dimensionOrder, c.id as criterionId, c.name as criterionName,
                   q.id as questionId, q.code as questionCode, r.role as role,
                   ql.score as score
            from SurveyAnswer a
            join a.response r
            join a.question q
            join q.criterion c
            join c.dimension d
            join q.levels ql on ql.levelNumber = a.selectedLevel
            where a.skipped = false and a.selectedLevel is not null
              and q.survey.id = :surveyId
            order by d.displayOrder, c.name, q.displayOrder, r.role
            """)
    List<DimensionScoreRow> findDimensionScoreRowsBySurveyId(@Param("surveyId") Long surveyId);

    interface DimensionScoreRow {
        Long getDimensionId();
        String getDimensionKey();
        String getDimensionLabel();
        int getDimensionOrder();
        Long getCriterionId();
        String getCriterionName();
        Long getQuestionId();
        String getQuestionCode();
        org.mapnaom.surveyappbackend.entity.SurveyRole getRole();
        double getScore();
    }
}
