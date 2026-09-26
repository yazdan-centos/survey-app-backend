package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Criterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface CriterionRepository extends JpaRepository<Criterion, UUID> {
    Optional<Criterion> findByDimensionIdAndName(UUID dimensionId, String name);
    List<Criterion> findByDimensionId(UUID dimensionId);
    boolean existsByDimensionId(UUID dimensionId);
    Boolean existsByDimensionIdAndName(UUID dimensionId, String name);
}
