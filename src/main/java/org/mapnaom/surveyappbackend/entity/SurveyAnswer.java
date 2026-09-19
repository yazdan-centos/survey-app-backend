/*
 * SurveyResponse represents one survey submission: it stores the respondent's
 * role, optional username, submission time, demographic answers, and question
 * answers. Use it to group and retrieve the data belonging to a submission.
 *
 * SurveyAnswer represents the answer to one Question within that submission.
 * It records the selected level and whether the question was skipped, and links
 * the question to its parent SurveyResponse. Use it for question-level results
 * and analysis of selected levels or skipped questions.
 *
 * One SurveyResponse can contain many SurveyAnswer records, but each
 * SurveyAnswer belongs to exactly one response and one question. The unique
 * (response_id, question_id) constraint allows at most one answer per question
 * in a submission. For example, a submission with ten recorded question answers
 * has one SurveyResponse and ten SurveyAnswer records; demographic answers are
 * stored separately as DemographicAnswer records.
 */
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
@Table(name = "survey_answers", uniqueConstraints = @UniqueConstraint(columnNames = {"response_id", "question_id"}))
public class SurveyAnswer extends BaseEntity {
    @Column
    private Integer selectedLevel;

    @Column(nullable = false)
    private boolean skipped;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "response_id", nullable = false)
    private SurveyResponse response;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
