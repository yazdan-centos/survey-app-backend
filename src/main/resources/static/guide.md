# Survey Application Developer Guide

This guide is a short glossary for developers who are new to survey systems and this application's domain model. The terms below describe the JPA entities in `org.mapnaom.surveyappbackend.entity`.

## How the model fits together

```text
Survey
  └── Questions ── belong to ── Criterion ── belongs to ── Dimension
         └── QuestionLevels

SurveyResponse
  ├── SurveyAnswers ── reference Questions
  └── DemographicAnswers
```

`Survey` is the definition of a questionnaire. `SurveyResponse` is one person's completed (or being completed) set of answers to that questionnaire. A response is not directly linked to a `Survey` entity; its answers identify the questions being answered.

## Shared entity fields

Every entity extends `BaseEntity`, which supplies:

- `id`: a generated UUID primary key.
- `createdAt`: timestamp set when the row is first persisted.
- `updatedAt`: timestamp refreshed when the row changes.

These timestamps are managed by JPA lifecycle callbacks. New entities should normally leave them unset.

## Glossary

### Survey

A versioned questionnaire template.

- `title`: human-readable survey name.
- `version`: required and unique identifier for the survey version (for example, `2026.1`).
- `active`: whether the survey is currently active; new surveys default to `true`.
- `questions`: the questions that make up this survey.

The database does not currently enforce that only one survey version is active.

### Question

One prompt in a survey for a particular audience.

- `code`: short identifier. It must be unique within the combination of survey, `role`, and code.
- `criterion`: required parent criterion (a `Criterion` entity).
- `text`: the prompt shown to the respondent.
- `role`: intended respondent audience, represented by `SurveyRole`.
- `displayOrder`: position when presenting questions.
- `survey`: required parent survey.
- `levels`: possible rating levels for this question, ordered by `levelNumber`.

Questions require both a survey and a criterion before they can be stored successfully.

### QuestionLevel

One selectable rating or maturity level for a question.

- `levelNumber`: ordering number; unique per question.
- `description`: required explanation of what the level means.
- `title`: numeric/title value used by the Excel model.
- `score`: numeric score associated with the level.
- `question`: required parent question.

Deleting a question cascades to its levels. The `levelOrder` Excel property is currently not persisted; `getLevelOrder()` always returns `0`.

### Dimension

A reusable grouping or area of assessment, such as “Leadership” or “Operations”.

- `key`: stable unique identifier used by code or integrations.
- `label`: display name.
- `displayOrder`: position when dimensions are displayed.
- `criteria`: criteria assigned to this dimension.

Dimensions are independent entities and can contain many criteria.

### Criterion

A named assessment criterion within a dimension.

- `name`: required name, unique within its dimension.
- `dimension`: required parent dimension.
- `questions`: questions assigned to this criterion.

Question creation requests identify an existing criterion using `criterionId`. Question Excel templates and exports include a `criterion_id` column containing that UUID.

### SurveyRole

The audience for a question or response:

| Value | Meaning | May skip? |
| --- | --- | --- |
| `MANAGERS` | Manager respondents | No |
| `BOARD` | Board respondents | Yes |
| `CUSTOMERS` | Customer respondents | Yes |
| `SUPPLIERS` | Supplier respondents | Yes |

The `allowsSkipping()` method exposes this rule. The response-submission API that would enforce it has not yet been implemented.

### SurveyResponse

One respondent's survey session or submission.

- `role`: audience represented by the response.
- `respondentUsername`: optional username of the respondent.
- `submittedAt`: required submission timestamp.
- `answers`: question-level answers belonging to this response.
- `demographics`: free-form demographic field/value answers.

Answers and demographics are cascade-persisted and orphan-removed with their response.

### SurveyAnswer

The answer to one question within a response.

- `selectedLevel`: selected `QuestionLevel.levelNumber`; nullable when the question is skipped.
- `skipped`: whether the respondent skipped the question.
- `response`: required parent response.
- `question`: required question being answered.

The database allows only one answer for each response/question pair. Application code should keep `selectedLevel` and `skipped` consistent with the question's available levels and audience rules.

### DemographicAnswer

A flexible field/value pair attached to a response, for information such as department, region, or company size.

- `fieldKey`: name of the demographic field.
- `value`: stored text value.
- `response`: required parent response.

There is intentionally no fixed demographic enum, so clients and reporting code must agree on field-key names.

### User

An authenticated application user, either local or synchronized from LDAP/Active Directory.

- Identity/profile fields: `username`, names, `displayName`, `email`, `employeeId`, `department`, and `dn`.
- `role`: application permission role (`UserRole`).
- `password`: BCrypt-hashed password for local users; LDAP users authenticate against the directory.
- `enabled`: whether login is allowed.
- `deleted`: soft-delete/synchronization marker.
- `ldapUser`: identifies directory-sourced users.

### UserRole

Application roles currently defined by the model:

- `ADMIN`: system administrator.
- `SURVEY_ADMIN`: manages survey content.
- `USER`: standard application user.

Spring Security exposes these as authorities named `ROLE_ADMIN`, `ROLE_SURVEY_ADMIN`, and `ROLE_USER`.

## Relationship and persistence rules

- A survey has many questions; each question belongs to exactly one survey.
- A dimension has many questions; each question requires exactly one dimension.
- A question has many levels; level numbers must be unique within that question.
- A response has many survey answers and demographic answers.
- A survey answer references exactly one response and one question, with a unique response/question pair.
- All entity relationships use UUID identifiers and database foreign keys.

## Developer tips

Use enum names exactly as declared when sending or importing data; values are persisted as strings. Set both sides of relationships when constructing an object graph (for example, assign `question.setSurvey(survey)` and add the question to `survey.getQuestions()`). Before persisting questions or levels, populate every non-null field, especially `Criterion.dimension`, `Criterion.name`, `Question.criterion`, `Question.text`, and `QuestionLevel.description`.

