package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface SurveyRepository extends JpaRepository<Survey, UUID> {
}