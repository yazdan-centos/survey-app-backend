package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Criterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CriterionRepository extends JpaRepository<Criterion, Long> {
    Optional<Criterion> findByDimensionIdAndName(Long dimensionId, String name);
    List<Criterion> findByDimensionId(Long dimensionId);
    boolean existsByDimensionId(Long dimensionId);
    Boolean existsByDimensionIdAndName(Long dimensionId, String name);
}
