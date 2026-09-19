package org.mapnaom.surveyappbackend.repository;

import jakarta.persistence.LockModeType;
import org.mapnaom.surveyappbackend.entity.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SurveyResponse r where r.id = :id")
    Optional<SurveyResponse> findForUpdate(@Param("id") UUID id);
}
