package org.mapnaom.surveyappbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "question_levels", uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "level_number"}))
public class QuestionLevel extends BaseEntity {
    @Column(name = "level_number", nullable = false)
    private int levelNumber;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    private double title;
    private double score;

    public void setLevelOrder(Integer levelOrder) {
        // This method is intentionally left empty to ignore the levelOrder field
        // levelOrder is not stored in the database
    }

        public double getLevelOrder() {
        // This method is intentionally left empty to ignore the levelOrder field
        // levelOrder is not stored in the database
        return 0;
    }
}
