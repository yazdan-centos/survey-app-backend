package org.mapnaom.surveyappbackend.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.mapnaom.surveyappbackend.dto.question.QuestionSearchRequest;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.QuestionLevel;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SurveyQuestionSpecification {
    private SurveyQuestionSpecification() {
    }

    public static Specification<Question> search(QuestionSearchRequest filter) {
        return (root, query, cb) -> {
            if (filter == null) return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();
            equal(predicates, cb, root.get("id"), filter.getId());
            like(predicates, cb, root.get("code"), filter.getCode());
            like(predicates, cb, root.get("text"), filter.getText());
            equal(predicates, cb, root.get("role"), filter.getRole());
            equal(predicates, cb, root.get("displayOrder"), filter.getDisplayOrder());

            Path<?> survey = root.get("survey");
            equal(predicates, cb, survey.get("id"), filter.getSurveyId());
            like(predicates, cb, survey.get("title"), filter.getSurveyTitle());
            like(predicates, cb, survey.get("version"), filter.getSurveyVersion());
            equal(predicates, cb, survey.get("active"), filter.getSurveyActive());

            Path<?> criterion = root.get("criterion");
            equal(predicates, cb, criterion.get("id"), filter.getCriterionId());
            like(predicates, cb, criterion.get("name"), filter.getCriterionName());
            Path<?> dimension = criterion.get("dimension");
            equal(predicates, cb, dimension.get("id"), filter.getDimensionId());
            like(predicates, cb, dimension.get("key"), filter.getDimensionKey());
            like(predicates, cb, dimension.get("label"), filter.getDimensionLabel());
            equal(predicates, cb, dimension.get("displayOrder"), filter.getDimensionDisplayOrder());

            range(predicates, cb, root.get("createdAt"), filter.getCreatedAtFrom(), filter.getCreatedAtTo());
            range(predicates, cb, root.get("updatedAt"), filter.getUpdatedAtFrom(), filter.getUpdatedAtTo());

            if (filter.getLevelId() != null || filter.getLevelNumber() != null
                    || hasText(filter.getDescription()) || filter.getLevelTitle() != null
                    || filter.getLevelScore() != null) {
                predicates.add(matchingLevel(root, query, cb, filter));
            }
            if (hasText(filter.getQ())) {
                List<Predicate> matches = new ArrayList<>();
                like(matches, cb, root.get("code"), filter.getQ());
                like(matches, cb, root.get("text"), filter.getQ());
                like(matches, cb, survey.get("title"), filter.getQ());
                like(matches, cb, survey.get("version"), filter.getQ());
                like(matches, cb, criterion.get("name"), filter.getQ());
                like(matches, cb, dimension.get("key"), filter.getQ());
                like(matches, cb, dimension.get("label"), filter.getQ());
                QuestionSearchRequest levelFilter = new QuestionSearchRequest();
                levelFilter.setDescription(filter.getQ());
                matches.add(matchingLevel(root, query, cb, levelFilter));
                predicates.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate matchingLevel(Root<Question> root, CriteriaQuery<?> query,
                                           CriteriaBuilder cb, QuestionSearchRequest filter) {
        // EXISTS keeps pagination/counts correct when several levels match a question.
        var subquery = query.subquery(Integer.class);
        Root<QuestionLevel> level = subquery.from(QuestionLevel.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(level.get("question"), root));
        equal(predicates, cb, level.get("id"), filter.getLevelId());
        equal(predicates, cb, level.get("levelNumber"), filter.getLevelNumber());
        like(predicates, cb, level.get("description"), filter.getDescription());
        equal(predicates, cb, level.get("title"), filter.getLevelTitle());
        equal(predicates, cb, level.get("score"), filter.getLevelScore());
        subquery.select(cb.literal(1)).where(predicates.toArray(Predicate[]::new));
        return cb.exists(subquery);
    }

    private static void equal(List<Predicate> predicates, CriteriaBuilder cb, Path<?> path, Object value) {
        if (value != null) predicates.add(cb.equal(path, value));
    }

    private static void like(List<Predicate> predicates, CriteriaBuilder cb, Path<String> path, String value) {
        if (hasText(value)) {
            String pattern = "%" + value.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            predicates.add(cb.like(cb.lower(path), pattern, '\\'));
        }
    }

    private static void range(List<Predicate> predicates, CriteriaBuilder cb, Path<Instant> path,
                              Instant from, Instant to) {
        if (from != null) predicates.add(cb.greaterThanOrEqualTo(path, from));
        if (to != null) predicates.add(cb.lessThanOrEqualTo(path, to));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
