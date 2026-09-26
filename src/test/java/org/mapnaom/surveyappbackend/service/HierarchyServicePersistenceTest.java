package org.mapnaom.surveyappbackend.service;

import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.mapnaom.surveyappbackend.dto.dimension.SaveDimensionRequest;
import org.mapnaom.surveyappbackend.dto.criterion.SaveCriterionRequest;
import org.mapnaom.surveyappbackend.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@Import({DimensionService.class, CriterionService.class})
class HierarchyServicePersistenceTest {
    @Autowired DimensionService dimensions;
    @Autowired CriterionService criteria;
    @Autowired EntityManager entityManager;

    @Test
    void createsReadsUpdatesMovesAndDeletesHierarchy() {
        Dimension first = dimensions.create(dimension("first"));
        Dimension second = dimensions.create(dimension("second"));
        Criterion criterion = criteria.create(criterion(first.getId(), "Quality"));
        entityManager.clear();
        assertThat(dimensions.findAll()).hasSize(2);
        assertThat(criteria.findAll()).hasSize(1);
        assertThat(criteria.findByDimension(first.getId())).extracting(Criterion::getId)
                .containsExactly(criterion.getId());
        assertThat(dimensions.findById(first.getId()).getCriteria()).hasSize(1);

        var changed = dimension("renamed");
        changed.setLabel("Updated label");
        changed.setDisplayOrder(2);
        dimensions.update(first.getId(), changed);
        criteria.update(criterion.getId(), criterion(second.getId(), "Updated criterion"));
        entityManager.clear();
        assertThat(dimensions.findById(first.getId()).getKey()).isEqualTo("renamed");
        assertThat(dimensions.findById(first.getId()).getLabel()).isEqualTo("Updated label");
        assertThat(dimensions.findById(first.getId()).getDisplayOrder()).isEqualTo(2);
        assertThat(criteria.findById(criterion.getId()).getName()).isEqualTo("Updated criterion");
        assertThat(criteria.findByDimension(first.getId())).isEmpty();
        assertThat(criteria.findByDimension(second.getId())).hasSize(1);

        criteria.delete(criterion.getId());
        dimensions.delete(first.getId());
        dimensions.delete(second.getId());
        assertThat(criteria.findAll()).isEmpty();
        assertThat(dimensions.findAll()).isEmpty();
    }

    @Test
    void missingRecordsAndParentsReturnNotFound() {
        UUID missing = UUID.randomUUID();
        assertStatus(() -> dimensions.findById(missing), HttpStatus.NOT_FOUND);
        assertStatus(() -> dimensions.update(missing, dimension("missing")), HttpStatus.NOT_FOUND);
        assertStatus(() -> dimensions.delete(missing), HttpStatus.NOT_FOUND);
        assertStatus(() -> criteria.findById(missing), HttpStatus.NOT_FOUND);
        assertStatus(() -> criteria.update(missing, criterion(missing, "Missing")), HttpStatus.NOT_FOUND);
        assertStatus(() -> criteria.delete(missing), HttpStatus.NOT_FOUND);
        assertStatus(() -> criteria.findByDimension(missing), HttpStatus.NOT_FOUND);
        assertStatus(() -> criteria.create(criterion(missing, "Missing")), HttpStatus.NOT_FOUND);
    }

    @Test
    void duplicateDimensionKeyReturnsConflict() {
        dimensions.create(dimension("quality"));
        assertStatus(() -> dimensions.create(dimension("quality")), HttpStatus.CONFLICT);
    }

    @Test
    void duplicateCriterionNameWithinDimensionReturnsConflict() {
        Dimension parent = dimensions.create(dimension("quality"));
        criteria.create(criterion(parent.getId(), "Quality"));
        assertStatus(() -> criteria.create(criterion(parent.getId(), "Quality")), HttpStatus.CONFLICT);
    }

    @Test
    void sameCriterionNameCanBeUsedInDifferentDimensions() {
        criteria.create(criterion(dimensions.create(dimension("first")).getId(), "Quality"));
        criteria.create(criterion(dimensions.create(dimension("second")).getId(), "Quality"));
        assertThat(criteria.findAll()).hasSize(2);
    }

    @Test
    void dimensionWithCriteriaCannotBeDeleted() {
        Dimension parent = dimensions.create(dimension("quality"));
        criteria.create(criterion(parent.getId(), "Quality"));
        assertStatus(() -> dimensions.delete(parent.getId()), HttpStatus.CONFLICT);
    }

    @Test
    void criterionWithQuestionsCannotBeDeleted() {
        Criterion criterion = criteria.create(criterion(dimensions.create(dimension("quality")).getId(), "Quality"));
        Survey survey = new Survey();
        survey.setTitle("Survey");
        survey.setVersion("test");
        entityManager.persist(survey);
        Question question = new Question();
        question.setCode("Q1");
        question.setText("Question");
        question.setRole(SurveyRole.MANAGERS);
        question.setSurvey(survey);
        question.setCriterion(criterion);
        entityManager.persist(question);
        entityManager.flush();
        assertStatus(() -> criteria.delete(criterion.getId()), HttpStatus.CONFLICT);
    }

    private SaveDimensionRequest dimension(String key) {
        var request = new SaveDimensionRequest();
        request.setKey(key);
        request.setLabel("Quality");
        request.setDisplayOrder(1);
        return request;
    }

    private SaveCriterionRequest criterion(UUID dimensionId, String name) {
        var request = new SaveCriterionRequest();
        request.setDimensionId(dimensionId);
        request.setName(name);
        return request;
    }

    private void assertStatus(ThrowingCallable action, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }
}
