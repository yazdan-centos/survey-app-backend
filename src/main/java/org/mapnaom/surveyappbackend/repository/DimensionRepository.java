package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DimensionRepository extends JpaRepository<Dimension, Long> {
    Optional<Dimension> findByKey(String key);
}
