package org.mapnaom.surveyappbackend.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
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
@Table(
        name = "demographic_questions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"group_key", "display_order"})
)
public class DemoGraphicQuestion extends BaseEntity {
    @Column(name = "group_key", nullable = false, length = 30)
    private String groupKey;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(length = 20)
    private String type;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "demographic_question_options",
            joinColumns = @JoinColumn(name = "question_id")
    )
    @OrderColumn(name = "option_order")
    @Column(name = "option_text", nullable = false, columnDefinition = "text")
    private List<String> options = new ArrayList<>();
}
