package org.mapnaom.surveyappbackend.mapper;

import org.mapnaom.surveyappbackend.entity.Criterion;
import org.mapnaom.surveyappbackend.dto.CriterionDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CriterionMapper {
    @Mapping(source = "dimensionId", target = "dimension.id")
    Criterion toEntity(CriterionDto criterionDto);

    @Mapping(source = "dimension.id", target = "dimensionId")
    CriterionDto toDto(Criterion criterion);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "dimensionId", target = "dimension.id")
    Criterion partialUpdate(CriterionDto criterionDto, @MappingTarget Criterion criterion);
}