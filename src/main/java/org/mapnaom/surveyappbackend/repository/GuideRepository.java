package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Long> {
}
