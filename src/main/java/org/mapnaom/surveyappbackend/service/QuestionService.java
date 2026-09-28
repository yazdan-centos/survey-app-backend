package org.mapnaom.surveyappbackend.service;

import lombok.RequiredArgsConstructor;
import org.mapnaom.surveyappbackend.dto.question.CreateQuestionLevelRequest;
import org.mapnaom.surveyappbackend.dto.question.CreateQuestionRequest;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.Criterion;
import org.mapnaom.surveyappbackend.repository.CriterionRepository;
import org.mapnaom.surveyappbackend.entity.QuestionLevel;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapnaom.surveyappbackend.repository.QuestionRepository;
import org.mapnaom.surveyappbackend.repository.SurveyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mapnaom.surveyappbackend.dto.question.QuestionResponseDto;
import org.mapnaom.surveyappbackend.dto.question.QuestionSearchRequest;
import org.mapnaom.surveyappbackend.specification.SurveyQuestionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SurveyRepository surveyRepository;
    private final CriterionRepository criterionRepository;

    private static final Set<String> SORT_FIELDS = Set.of("id", "code", "text", "role", "displayOrder",
            "createdAt", "updatedAt", "survey.id", "survey.title", "survey.version", "survey.active",
            "criterion.id", "criterion.name", "criterion.dimension.id", "criterion.dimension.key",
            "criterion.dimension.label", "criterion.dimension.displayOrder");

    @Transactional(readOnly = true)
    public Page<QuestionResponseDto> search(QuestionSearchRequest filter, Pageable pageable) {
        if (filter != null) {
            validateRange(filter.getCreatedAtFrom(), filter.getCreatedAtTo(), "createdAt");
            validateRange(filter.getUpdatedAtFrom(), filter.getUpdatedAtTo(), "updatedAt");
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported question sort field");
            }
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("displayOrder");
        if (sort.getOrderFor("id") == null) sort = sort.and(Sort.by("id"));
        Pageable bounded = PageRequest.of(pageable.isPaged() ? pageable.getPageNumber() : 0,
                pageable.isPaged() ? Math.min(pageable.getPageSize(), 200) : 20, sort);
        return questionRepository.findAll(SurveyQuestionSpecification.search(filter), bounded)
                .map(QuestionResponseDto::from);
    }

    private static void validateRange(Instant from, Instant to, String field) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + "From must be before or equal to " + field + "To");
        }
    }

    public Question create(CreateQuestionRequest request) {
        Survey survey = surveyRepository.findById(request.getSurveyId())
                .orElseThrow(() -> new RuntimeException("Survey not found"));

        Criterion criterion = criterionRepository.findById(request.getCriterionId())
                .orElseThrow(() -> new RuntimeException("Criterion not found"));

        Question question = new Question();
        question.setSurvey(survey);
        question.setCode(request.getCode());
        question.setCriterion(criterion);
        question.setText(request.getText());
        question.setRole(request.getRole());

        List<QuestionLevel> levels = new ArrayList<>();
        if (request.getLevels() != null) {
            for (CreateQuestionLevelRequest levelRequest : request.getLevels()) {
                QuestionLevel level = new QuestionLevel();
                level.setDescription(levelRequest.getTitle());
                level.setLevelNumber(levelRequest.getScore());
                level.setLevelOrder(levelRequest.getLevelOrder());
                level.setQuestion(question);
                levels.add(level);
            }
        }

        question.setLevels(levels);
        return questionRepository.save(question);
    }

    public List<Question> getBySurvey(UUID surveyId) {
        return questionRepository.findBySurveyId(surveyId);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponseDto> getSurveyQuestions(UUID surveyId) {
        return questionRepository.findBySurveyId(surveyId).stream().map(QuestionResponseDto::from).toList();
    }
}
