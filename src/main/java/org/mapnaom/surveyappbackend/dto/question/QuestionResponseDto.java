package org.mapnaom.surveyappbackend.dto.question;

import org.mapnaom.surveyappbackend.dto.dimension.DimensionResponseDto;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapnaom.surveyappbackend.entity.SurveyRole;

import java.util.Comparator;
import java.util.List;

public record QuestionResponseDto(Long id, Long surveyId, String code, String text, SurveyRole role,
                                  int displayOrder, Long criterionId, String criterionName,
                                  DimensionResponseDto dimension, List<Level> levels) {
    public record Level(Long id, int levelNumber, String description) {}

    public static QuestionResponseDto from(Question question) {
        return new QuestionResponseDto(question.getId(), question.getSurvey().getId(), question.getCode(),
                question.getText(), question.getRole(), question.getDisplayOrder(),
                question.getCriterion().getId(), question.getCriterion().getName(),
                DimensionResponseDto.from(question.getCriterion().getDimension()),
                question.getLevels().stream()
                        .map(level -> new Level(level.getId(), level.getLevelNumber(), level.getDescription()))
                        .sorted(Comparator.comparingInt(Level::levelNumber)).toList());
    }
}
