# Survey admin dashboard API

```http
GET /api/surveys/dashboard
Authorization: Bearer <access-token>
```

Requires `ROLE_SURVEY_ADMIN` or `ROLE_ADMIN`. Ordinary users receive 403. The endpoint returns 200 with JSON and `Cache-Control: no-store`. It accepts no parameters and reports all-time metrics across active and inactive surveys.

## Response shape

```json
{
  "generatedAt": "2026-09-19T00:00:00Z",
  "summary": {
    "totalSurveys": 1,
    "activeSurveys": 1,
    "inactiveSurveys": 0,
    "totalQuestions": 2,
    "totalResponses": 3,
    "unassignedResponses": 1,
    "totalAnswers": 3,
    "answeredAnswers": 2,
    "skippedAnswers": 1,
    "skipRate": 33.33
  },
  "surveys": [
    {
      "id": "00000000-0000-0000-0000-000000000001",
      "title": "Annual survey",
      "version": "2026-v1",
      "active": true,
      "createdAt": "2026-09-01T10:00:00Z",
      "updatedAt": "2026-09-01T10:00:00Z",
      "questionCount": 2,
      "responseCount": 2,
      "answerCount": 3,
      "answeredCount": 2,
      "skippedCount": 1,
      "skipRate": 33.33,
      "lastSubmittedAt": "2026-09-18T12:00:00Z",
      "audiences": [
        {"role": "MANAGERS", "responseCount": 0, "answerCount": 0, "answeredCount": 0, "skippedCount": 0, "skipRate": 0.00},
        {"role": "BOARD", "responseCount": 2, "answerCount": 3, "answeredCount": 2, "skippedCount": 1, "skipRate": 33.33},
        {"role": "CUSTOMERS", "responseCount": 0, "answerCount": 0, "answeredCount": 0, "skippedCount": 0, "skipRate": 0.00},
        {"role": "SUPPLIERS", "responseCount": 0, "answerCount": 0, "answeredCount": 0, "skippedCount": 0, "skipRate": 0.00}
      ]
    }
  ]
}
```

## Metric definitions

- `totalResponses` counts stored submissions, including those with no answers. Multiple submissions by the same respondent are counted separately. It is not a unique-person or completed-submission count.
- `unassignedResponses` counts responses with no answers. The current response entity has no direct survey reference, so these cannot be assigned to a survey.
- A survey's `responseCount` counts distinct responses linked through its questions' answers. Multiple answers from one response count once. Audience breakdowns use the response's `SurveyRole`, not the user's security role.
- `answerCount` includes skipped answers. `answeredCount` counts answers with `skipped=false`. Unanswered questions with no answer record are not counted as skipped.
- `skipRate` is `skippedCount / answerCount * 100`, rounded to two decimal places. It is zero when no answers exist. Summary rates use aggregate counts rather than averaging survey percentages.
- `lastSubmittedAt` is the latest original submission timestamp linked to that survey, or null when none exists. Answer edits do not change submission time.
- Surveys with no questions or responses are included. All four audiences are always present in enum order with zero defaults. Surveys are ordered by creation time descending, then ID ascending.
- Current submission validation keeps answers within one survey. If historical data contains a response spanning several surveys, it counts once in each referenced survey and once in the global total.

The response contains survey metadata and aggregate counts only; it does not expose respondent usernames, demographics or individual answers. No score or completion rate is inferred from selected level numbers.

## React consumption

```javascript
export async function loadSurveyDashboard(apiBaseUrl, accessToken, signal) {
  const response = await fetch(`${apiBaseUrl}/api/surveys/dashboard`, {
    headers: { Authorization: `Bearer ${accessToken}`, Accept: "application/json" },
    cache: "no-store",
    signal,
  });
  if (!response.ok) {
    throw new Error(`Could not load survey dashboard (${response.status})`);
  }
  return response.json();
}
```

Use `summary` for metric cards, `surveys` for the survey table, and a selected survey's `audiences` for audience charts. Use `survey.id` and `audience.role` as React keys. Display a placeholder for null `lastSubmittedAt`.

The backend uses four aggregate queries in a read-only, repeatable-read transaction, without loading response or answer entity collections. Integration tests verify empty data, distinct counts, audience breakdowns, skipped answers, unattributed responses and the fixed query count. MVC security tests cover both admin roles and denied access.

```powershell
.\mvnw.cmd "-Dtest=SurveyDashboardServiceTest,SurveyDashboardControllerTest,UserApiSecurityTest" test
```

Database tests run against isolated H2, not the configured PostgreSQL instance.
