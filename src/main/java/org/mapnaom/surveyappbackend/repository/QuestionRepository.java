package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {
    boolean existsByCriterionId(Long criterionId);
    List<Question> findBySurveyId(Long surveyId);
}
