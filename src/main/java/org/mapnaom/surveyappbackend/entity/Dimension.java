package org.mapnaom.surveyappbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dimensions")
public class Dimension extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String key;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(nullable = false)
    private int displayOrder;

    @OneToMany(mappedBy = "dimension")
    private List<Criterion> criteria = new ArrayList<>();
}
