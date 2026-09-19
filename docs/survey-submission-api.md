# Survey response and answer API

Authentication is required. Respondents can read and update their own submissions; `ADMIN` and `SURVEY_ADMIN` can manage any submission. The server sets the respondent username from authentication and the submission time on creation. Updates preserve both.

| Method | Path | Action |
| --- | --- | --- |
| POST | `/api/survey-responses` | Create a response with children; 201 |
| GET | `/api/survey-responses/{id}` | Read a response |
| PUT | `/api/survey-responses/{id}` | Replace role, answers and demographics |
| POST | `/api/survey-responses/{responseId}/answers` | Add an answer; 201 |
| GET | `/api/survey-responses/{responseId}/answers` | List answers |
| GET | `/api/survey-responses/{responseId}/answers/{answerId}` | Read an answer |
| PUT | `/api/survey-responses/{responseId}/answers/{answerId}` | Update an answer |

Creation returns generated IDs and a Location header. Read/update operations return 200. DTOs avoid recursive entity serialization.

## Response request

Use this shape for POST and PUT, replacing the question ID with an existing BOARD question:

```json
{
  "role": "BOARD",
  "answers": [
    {"questionId": "00000000-0000-0000-0000-000000000001", "selectedLevel": 2, "skipped": false}
  ],
  "demographics": [
    {"fieldKey": "department", "value": "Engineering"}
  ]
}
```

Role is required: MANAGERS, BOARD, CUSTOMERS or SUPPLIERS. Every question must match the response role and belong to the same survey.

PUT replaces both child collections. Answers are matched by questionId, demographics by fieldKey; matching children keep their IDs. Omitted children are deleted. Omitted collections default to empty; explicit null collections/elements are invalid. Include every child to retain, or use the individual-answer endpoint to edit just one answer.

Empty responses are allowed for adding answers individually. The existing model has no draft/final state or direct survey link on a response, so completeness and active-survey restrictions are not enforced. The survey is inferred through the answers. Full replacement validates the new set; individual additions must match existing answers' survey.

## Individual answer request

```json
{
  "questionId": "00000000-0000-0000-0000-000000000001",
  "selectedLevel": 2,
  "skipped": false
}
```

questionId is required. selectedLevel identifies QuestionLevel.levelNumber, not its UUID, score or title. A non-skipped answer requires a valid positive level number. A skipped answer requires a null/omitted level. skipped defaults to false and cannot be null. MANAGERS cannot skip; other audiences can.

Each response allows one answer per question. Adding a duplicate returns 409. An existing answer's question and parent cannot be changed; an answer ID scoped to the wrong response returns 404.

Demographic keys must be nonblank, unique within the response and at most 100 characters. Values must be nonblank.

## Persistence and errors

Writes are transactional; validation failures preserve the previously persisted data. Updates and individual-answer writes lock the parent response to serialize concurrent mutations. Full replacement uses the supplied content; fetch current data before editing it.

Errors: 400 invalid input, 403 another respondent's submission, 404 missing response/question/scoped answer, 409 duplicate answer or database integrity conflict. Service errors return problem-detail JSON with a detail field.

Submission DTOs are in dto.response. The existing dto.survey.SurveyResponseDto continues to describe survey metadata.

## Tests

```powershell
.\mvnw.cmd "-Dtest=SurveySubmissionPersistenceTest,SurveySubmissionControllerTest" test
```

23 tests cover routing, validation, ownership, real H2 transactions, rollback, IDs and orphan removal. PostgreSQL is not accessed by these tests.
