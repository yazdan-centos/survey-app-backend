package org.mapnaom.surveyappbackend.mapper;

import org.mapnaom.surveyappbackend.dto.guide.GuideDto;
import org.mapnaom.surveyappbackend.entity.Guide;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface GuideMapper {
    @Mapping(source = "surveyVersion", target = "survey.version")
    @Mapping(source = "surveyId", target = "survey.id")
    Guide toEntity(GuideDto guideDto);

    @InheritInverseConfiguration(name = "toEntity")
    GuideDto toDto(Guide guide);

    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Guide partialUpdate(GuideDto guideDto, @MappingTarget Guide guide);
}