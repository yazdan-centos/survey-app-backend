package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DimensionRepository extends JpaRepository<Dimension, UUID> {
    Optional<Dimension> findByKey(String key);
}
