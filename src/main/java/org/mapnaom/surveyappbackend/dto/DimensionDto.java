package org.mapnaom.surveyappbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mapnaom.surveyappbackend.entity.Dimension;

import java.io.Serializable;

/**
 * DTO for {@link Dimension}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DimensionDto implements Serializable {
    private String key;
    private String label;
    private int displayOrder;
}