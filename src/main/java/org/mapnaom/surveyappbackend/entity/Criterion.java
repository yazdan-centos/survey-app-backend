package org.mapnaom.surveyappbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "criteria", uniqueConstraints = @UniqueConstraint(columnNames = {"dimension_id", "name"}))
public class Criterion extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dimension_id", nullable = false)
    private Dimension dimension;

    @OneToMany(mappedBy = "criterion")
    private List<Question> questions = new ArrayList<>();
}
