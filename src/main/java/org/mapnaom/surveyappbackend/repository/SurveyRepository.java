package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface SurveyRepository extends JpaRepository<Survey, UUID> {
    boolean existsByVersion(String version);
    Optional<Survey> findByVersion(String version);

    @Query("select s from Survey s where s.active = true")
    Optional<Survey> findByActiveTrue();
    }

