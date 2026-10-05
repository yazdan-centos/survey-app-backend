package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.QuestionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionLevelRepository extends JpaRepository<QuestionLevel, Long> {
}
