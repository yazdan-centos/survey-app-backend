package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    boolean existsByCriterionId(UUID criterionId);
    List<Question> findBySurveyId(UUID surveyId);
}
