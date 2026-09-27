package org.mapnaom.surveyappbackend.mapper;

import org.mapnaom.surveyappbackend.dto.QuestionLevelDto;
import org.mapnaom.surveyappbackend.entity.QuestionLevel;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface QuestionLevelMapper {
    @Mapping(source = "questionId", target = "question.id")
    QuestionLevel toEntity(QuestionLevelDto questionLevelDto);

    @Mapping(source = "question.id", target = "questionId")
    QuestionLevelDto toDto(QuestionLevel questionLevel);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "questionId", target = "question.id")
    QuestionLevel partialUpdate(QuestionLevelDto questionLevelDto, @MappingTarget QuestionLevel questionLevel);
}