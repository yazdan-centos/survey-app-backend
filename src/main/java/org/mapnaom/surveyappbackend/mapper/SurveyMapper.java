package org.mapnaom.surveyappbackend.mapper;

import org.mapnaom.surveyappbackend.dto.SurveyDto;
import org.mapnaom.surveyappbackend.entity.Survey;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SurveyMapper {
    Survey toEntity(SurveyDto surveyDto);

    SurveyDto toDto(Survey survey);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Survey partialUpdate(SurveyDto surveyDto, @MappingTarget Survey survey);
}