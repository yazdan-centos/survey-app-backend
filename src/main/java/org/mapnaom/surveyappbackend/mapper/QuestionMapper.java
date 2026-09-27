package org.mapnaom.surveyappbackend.mapper;

import org.mapnaom.surveyappbackend.dto.QuestionDto;
import org.mapnaom.surveyappbackend.entity.Question;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface QuestionMapper {
    @Mapping(source = "criterionId", target = "criterion.id")
    @Mapping(source = "surveyId", target = "survey.id")
    Question toEntity(QuestionDto questionDto);

    @Mapping(source = "criterion.id", target = "criterionId")
    @Mapping(source = "survey.id", target = "surveyId")
    QuestionDto toDto(Question question);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Question partialUpdate(QuestionDto questionDto, @MappingTarget Question question);


}