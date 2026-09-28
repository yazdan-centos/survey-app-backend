package org.mapnaom.surveyappbackend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapnaom.surveyappbackend.dto.question.QuestionResponseDto;
import org.mapnaom.surveyappbackend.dto.question.QuestionSearchRequest;
import org.mapnaom.surveyappbackend.entity.*;
import org.mapnaom.surveyappbackend.service.QuestionService;
import org.mapnaom.surveyappbackend.specification.SurveyQuestionSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
class QuestionSearchTest {
    @Autowired QuestionRepository repository;
    @Autowired SurveyRepository surveys;
    @Autowired CriterionRepository criteria;
    @Autowired DimensionRepository dimensions;

    private Survey survey;
    private Criterion criterion;
    private Dimension dimension;

    @BeforeEach
    void setUp() {
        survey = new Survey();
        survey.setTitle("Annual Review");
        survey.setVersion("2026");
        survey.setActive(false);
        surveys.saveAndFlush(survey);
        dimension = new Dimension();
        dimension.setKey("leadership");
        dimension.setLabel("Leadership dimension");
        dimension.setDisplayOrder(3);
        dimensions.saveAndFlush(dimension);
        criterion = new Criterion();
        criterion.setName("Strategy");
        criterion.setDimension(dimension);
        criteria.saveAndFlush(criterion);
    }

    @Test
    void filtersEveryQuestionFieldAndRelatedAttribute() {
        Question question = save("Q1", "Leadership quality", 0, "Excellent");
        QuestionLevel level = question.getLevels().get(0);
        level.setTitle(2.5);
        level.setScore(4.5);
        repository.saveAndFlush(question);

        List<Consumer<QuestionSearchRequest>> filters = List.of(
                f -> f.setId(question.getId()), f -> f.setCode(" q1 "),
                f -> f.setText("QUALITY"), f -> f.setRole(SurveyRole.BOARD),
                f -> f.setDisplayOrder(0), f -> f.setSurveyId(survey.getId()),
                f -> f.setSurveyTitle("ANNUAL"), f -> f.setSurveyVersion("2026"),
                f -> f.setSurveyActive(false), f -> f.setCriterionId(criterion.getId()),
                f -> f.setCriterionName("STRATEGY"), f -> f.setDimensionId(dimension.getId()),
                f -> f.setDimensionKey("LEADER"), f -> f.setDimensionLabel("DIMENSION"),
                f -> f.setDimensionDisplayOrder(3), f -> f.setLevelId(level.getId()),
                f -> f.setLevelNumber(1), f -> f.setDescription("EXCELLENT"),
                f -> f.setLevelTitle(2.5), f -> f.setLevelScore(4.5),
                f -> f.setCreatedAtFrom(Instant.EPOCH), f -> f.setCreatedAtTo(Instant.now().plusSeconds(60)),
                f -> f.setUpdatedAtFrom(Instant.EPOCH), f -> f.setUpdatedAtTo(Instant.now().plusSeconds(60)));
        QuestionSearchRequest combined = new QuestionSearchRequest();
        for (Consumer<QuestionSearchRequest> configure : filters) {
            QuestionSearchRequest filter = new QuestionSearchRequest();
            configure.accept(filter);
            configure.accept(combined);
            assertThat(repository.findAll(SurveyQuestionSpecification.search(filter)))
                    .extracting(Question::getId).containsExactly(question.getId());
        }
        assertThat(service().search(combined, PageRequest.of(0, 20)).getContent())
                .extracting(QuestionResponseDto::id).containsExactly(question.getId());
        combined.setSurveyActive(true);
        assertThat(service().search(combined, PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    void combinesFiltersAndCountsEachQuestionOnceAcrossPages() {
        save("Q1", "Leadership", 0, "Excellent", "Excellent progress");
        save("Q2", "Leadership", 1, "Excellent");
        save("Q3", "Other", 2, "Excellent");
        QuestionSearchRequest filter = new QuestionSearchRequest();
        filter.setText("LEADER");
        filter.setDescription("EXCELLENT");
        filter.setSurveyId(survey.getId());
        var first = service().search(filter, PageRequest.of(0, 1));
        var second = service().search(filter, PageRequest.of(1, 1));
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(QuestionResponseDto::code).containsExactly("Q1");
        assertThat(first.getContent().get(0).levels()).hasSize(2);
        assertThat(second.getContent()).extracting(QuestionResponseDto::code).containsExactly("Q2");
    }

    @Test
    void levelFiltersMustMatchTheSameLevel() {
        save("Q1", "Question", 1, "Basic", "Excellent");
        QuestionSearchRequest filter = new QuestionSearchRequest();
        filter.setLevelNumber(1);
        filter.setDescription("Excellent");
        assertThat(service().search(filter, PageRequest.of(0, 20))).isEmpty();
        filter.setLevelNumber(2);
        assertThat(service().search(filter, PageRequest.of(0, 20)).getContent()).hasSize(1);
    }

    @Test
    void freeTextSearchIncludesRelatedFieldsAndQuestionsWithoutLevels() {
        save("Q1", "Unique wording", 0);
        save("Q2", "Different", 1, "Unique description", "Unique description again");
        for (String q : List.of("annual", "2026", "strategy", "leadership", "dimension", "unique")) {
            QuestionSearchRequest filter = new QuestionSearchRequest();
            filter.setQ(q);
            assertThat(service().search(filter, PageRequest.of(0, 1)).getTotalElements())
                    .as("query %s", q).isEqualTo(2);
        }
        QuestionSearchRequest filter = new QuestionSearchRequest();
        filter.setQ("description");
        assertThat(service().search(filter, PageRequest.of(0, 1)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void ignoresBlankFiltersAndEscapesLiteralWildcards() {
        save("Q1", "Literal \\_%", 0, "Level \\_%");
        save("Q2", "Literal other", 1, "Level other");
        QuestionSearchRequest filter = new QuestionSearchRequest();
        filter.setCode("  ");
        filter.setQ("  ");
        assertThat(service().search(filter, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(2);
        assertThat(service().search(null, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(2);
        filter.setText("\\_%");
        filter.setDescription("\\_%");
        assertThat(service().search(filter, PageRequest.of(0, 20)).getContent())
                .extracting(QuestionResponseDto::code).containsExactly("Q1");
    }

    @Test
    void timestampBoundsExcludeQuestionsAndRejectReversedRanges() {
        save("Q1", "Question", 0);
        for (Consumer<QuestionSearchRequest> configure : List.<Consumer<QuestionSearchRequest>>of(
                f -> f.setCreatedAtFrom(Instant.now().plusSeconds(60)),
                f -> f.setCreatedAtTo(Instant.EPOCH),
                f -> f.setUpdatedAtFrom(Instant.now().plusSeconds(60)),
                f -> f.setUpdatedAtTo(Instant.EPOCH))) {
            QuestionSearchRequest filter = new QuestionSearchRequest();
            configure.accept(filter);
            assertThat(service().search(filter, PageRequest.of(0, 20))).isEmpty();
        }
        QuestionSearchRequest filter = new QuestionSearchRequest();
        filter.setCreatedAtFrom(Instant.now());
        filter.setCreatedAtTo(Instant.EPOCH);
        assertThatThrownBy(() -> service().search(filter, PageRequest.of(0, 20)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }

    @Test
    void boundsPageSizeAndValidatesSortFields() {
        var page = service().search(new QuestionSearchRequest(), PageRequest.of(0, 1000));
        assertThat(page.getSize()).isEqualTo(200);
        assertThat(page.getSort().getOrderFor("displayOrder")).isNotNull();
        assertThat(page.getSort().getOrderFor("id")).isNotNull();
        assertThat(service().search(null, Pageable.unpaged()).getSize()).isEqualTo(20);
        assertThatThrownBy(() -> service().search(null, PageRequest.of(0, 20, Sort.by("levels.description"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        save("Q1", "Question", 0);
        assertThat(service().search(null, PageRequest.of(0, 20, Sort.by("criterion.dimension.label")))
                .getContent()).hasSize(1);
    }

    private QuestionService service() {
        return new QuestionService(repository, surveys, criteria);
    }

    private Question save(String code, String text, int order, String... descriptions) {
        Question question = new Question();
        question.setSurvey(survey);
        question.setCriterion(criterion);
        question.setCode(code);
        question.setText(text);
        question.setDisplayOrder(order);
        question.setRole(SurveyRole.BOARD);
        for (int i = 0; i < descriptions.length; i++) {
            QuestionLevel level = new QuestionLevel();
            level.setQuestion(question);
            level.setLevelNumber(i + 1);
            level.setDescription(descriptions[i]);
            question.getLevels().add(level);
        }
        return repository.saveAndFlush(question);
    }
}
