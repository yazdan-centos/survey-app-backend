package org.mapnaom.surveyappbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mapnaom.surveyappbackend.entity.Criterion;

import java.io.Serializable;

/**
 * DTO for {@link Criterion}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CriterionDto implements Serializable {
    private String name;
    private Long dimensionId;
}
