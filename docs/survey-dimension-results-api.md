# Survey dimension results API

The admin results endpoint returns the same nested shape consumed by
`SurveyDimensionChart`: dimensions contain criteria, and criteria contain
question points. Scores are calculated from each selected `QuestionLevel.score`.
Skipped answers are excluded.

```http
GET /api/surveys/results?surveyId=<uuid>&role=BOARD
Authorization: Bearer <access-token>
```

The survey-specific shorthand is also supported:

```http
GET /api/surveys/<uuid>/results?role=BOARD
```

Both `surveyId` and `role` are optional. Without filters, the aggregate includes
all scored answers. The response also includes one dimension list for every
`SurveyRole`:

```json
{
  "surveyId": null,
  "generatedAt": "2026-10-03T12:00:00Z",
  "dimensions": [
    {
      "id": "customerFocus",
      "label": "تمرکز بر مشتری",
      "color": "#64748B",
      "value": 3.75,
      "criteria": [
        {
          "id": "criterion-uuid",
          "label": "رضایت مشتری",
          "value": 3.75,
          "points": [
            {"id": "question-uuid", "label": "1-1", "value": 3.75}
          ]
        }
      ]
    }
  ],
  "roles": [
    {"role": "MANAGERS", "dimensions": []},
    {"role": "BOARD", "dimensions": []},
    {"role": "CUSTOMERS", "dimensions": []},
    {"role": "SUPPLIERS", "dimensions": []}
  ]
}
```

The endpoint requires `ROLE_ADMIN` or `ROLE_SURVEY_ADMIN` and sends
`Cache-Control: no-store`.
