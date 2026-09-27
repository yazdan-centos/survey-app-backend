package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.DemoGraphicQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface DemoGraphicQuestionRepository extends JpaRepository<DemoGraphicQuestion, UUID>, JpaSpecificationExecutor<DemoGraphicQuestion> {
    @EntityGraph(attributePaths = "options")
    List<DemoGraphicQuestion> findAllByOrderByGroupKeyAscDisplayOrderAsc();

    @EntityGraph(attributePaths = "options")
    List<DemoGraphicQuestion> findByGroupKeyOrderByDisplayOrderAsc(String groupKey);

    @Override
    @EntityGraph(attributePaths = "options")
    Optional<DemoGraphicQuestion> findById(UUID id);

    boolean existsByGroupKeyAndDisplayOrder(String groupKey, int displayOrder);
}
