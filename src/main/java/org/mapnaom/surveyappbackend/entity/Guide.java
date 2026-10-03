package org.mapnaom.surveyappbackend.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
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
@Table(name = "guides")
public class Guide extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideAudience audience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideTheme theme = GuideTheme.INDIGO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideSeverity severity = GuideSeverity.INFO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideStatus status = GuideStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 255)
    private String subtitle;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, columnDefinition = "text")
    private String footer;

    @Column(name = "start_button_label", length = 100)
    private String startButtonLabel;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "guide_role_tags",
            joinColumns = @JoinColumn(name = "guide_id")
    )
    @OrderColumn(name = "tag_order")
    @Column(name = "role_tag", nullable = false, length = 100)
    private List<String> roleTags = new ArrayList<>();
}
