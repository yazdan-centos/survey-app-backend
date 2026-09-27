package org.mapnaom.surveyappbackend.mapper;

import org.mapnaom.surveyappbackend.dto.DimensionDto;
import org.mapnaom.surveyappbackend.entity.Dimension;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface DimensionMapper {
    Dimension toEntity(DimensionDto dimensionDto);

    DimensionDto toDto(Dimension dimension);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Dimension partialUpdate(DimensionDto dimensionDto, @MappingTarget Dimension dimension);
}