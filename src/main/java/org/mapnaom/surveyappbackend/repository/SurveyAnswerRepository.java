package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, UUID> {
    Optional<SurveyAnswer> findByIdAndResponseId(UUID id, UUID responseId);
}
