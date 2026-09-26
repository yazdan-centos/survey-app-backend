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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SurveyRepository surveyRepository;
    private final CriterionRepository criterionRepository;

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
}
