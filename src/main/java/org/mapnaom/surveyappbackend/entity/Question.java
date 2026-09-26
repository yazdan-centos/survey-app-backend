package org.mapnaom.surveyappbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "questions", uniqueConstraints = @UniqueConstraint(columnNames = {"survey_id", "role", "code"}))
public class Question extends BaseEntity {
    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyRole role;

    @Column(nullable = false)
    private int displayOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private Criterion criterion;

    @OneToMany(mappedBy = "question", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @OrderBy("levelNumber ASC")
    private List<QuestionLevel> levels = new ArrayList<>();
}
