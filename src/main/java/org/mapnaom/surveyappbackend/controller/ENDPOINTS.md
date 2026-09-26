# Controller endpoints

Endpoints are grouped alphabetically by entity, then sorted by path and HTTP method. This inventory covers the `controller` package.

## Criteria

| Method | Path | Handler | Description |
| --- | --- | --- | --- |
| GET | `/api/criteria` | `getAllCriteria` | List all criteria; optionally filter with `?dimensionId={dimensionId}`. |
| POST | `/api/criteria` | `createCriterion` | Create a criterion under an existing dimension. |
| DELETE | `/api/criteria/{criterionId}` | `deleteCriterion` | Delete a criterion with no questions. |
| GET | `/api/criteria/{criterionId}` | `getCriterionById` | Get one criterion. |
| PUT | `/api/criteria/{criterionId}` | `updateCriterion` | Replace the name and parent dimension of a criterion. |

POST and PUT require `name` (nonblank, maximum 200 characters) and `dimensionId` (UUID). Names must be unique within a dimension. Responses contain `id`, `name`, `dimensionId`, `createdAt`, and `updatedAt`.

Example request:

```json
{"name":"Leadership","dimensionId":"00000000-0000-0000-0000-000000000001"}
```

## Dimensions

| Method | Path | Handler | Description |
| --- | --- | --- | --- |
| GET | `/api/dimensions` | `getAllDimensions` | List all dimensions. |
| POST | `/api/dimensions` | `createDimension` | Create a dimension. |
| DELETE | `/api/dimensions/{dimensionId}` | `deleteDimension` | Delete a dimension with no criteria. |
| GET | `/api/dimensions/{dimensionId}` | `getDimensionById` | Get one dimension. |
| PUT | `/api/dimensions/{dimensionId}` | `updateDimension` | Replace a dimension's key, label, and display order. |

POST and PUT require `key` (nonblank, unique, maximum 80 characters), `label` (nonblank, maximum 200 characters), and `displayOrder` (integer). Responses contain `id`, `key`, `label`, `displayOrder`, `createdAt`, and `updatedAt`.

Example request:

```json
{"key":"strategicVision","label":"Strategic vision","displayOrder":1}
```

Dimension and criterion endpoints require authentication. Successful reads, creates, and updates return 200; deletes return 204. Invalid request fields return 400, missing records or parent dimensions return 404, and duplicate keys/names or deletion blocked by child records return 409. Responses omit child collections; use the criterion list filter to retrieve a dimension's criteria.

## Questions

| Method | Path | Handler | Description |
| --- | --- | --- | --- |
| POST | `/api/questions` | `createQuestion` | Create a question. |
| GET | `/api/questions/export?surveyId={surveyId}` | `exportQuestionsToExcel` | Export a survey's questions to an Excel file. |
| POST | `/api/questions/import?surveyId={surveyId}` | `importQuestionsFromExcel` | Import questions for a survey from a multipart Excel file. |
| GET | `/api/questions/survey/{surveyId}` | `getQuestionsBySurveyId` | List questions for a survey. |
| GET | `/api/questions/template` | `downloadQuestionImportTemplate` | Download the question import template. |

## Survey Answers

| Method | Path | Handler | Description |
| --- | --- | --- | --- |
| GET | `/api/survey-responses/{responseId}/answers` | `getSurveyAnswersByResponseId` | List the answers in a survey response. |
| POST | `/api/survey-responses/{responseId}/answers` | `createSurveyAnswer` | Add an answer to a survey response. |
| GET | `/api/survey-responses/{responseId}/answers/{answerId}` | `getSurveyAnswerById` | Get one answer from a survey response. |
| PUT | `/api/survey-responses/{responseId}/answers/{answerId}` | `updateSurveyAnswer` | Update one answer in a survey response. |

## Survey Responses

| Method | Path | Handler | Description |
| --- | --- | --- | --- |
| POST | `/api/survey-responses` | `createSurveyResponse` | Create a survey response. |
| GET | `/api/survey-responses/{responseId}` | `getSurveyResponseById` | Get a survey response. |
| PUT | `/api/survey-responses/{responseId}` | `updateSurveyResponse` | Update a survey response. |

## Surveys

| Method | Path | Handler | Description |
| --- | --- | --- | --- |
| GET | `/api/surveys/dashboard` | `getSurveyDashboard` | Get aggregate survey dashboard data. |
| GET | `/api/v1/surveys` | `getAllSurveys` | List all surveys. |
| POST | `/api/v1/surveys` | `createSurvey` | Create a survey. |
| GET | `/api/v1/surveys/active` | `getActiveSurvey` | Get the active survey. |
| DELETE | `/api/v1/surveys/{surveyId}` | `deleteSurvey` | Delete a survey. |
| PUT | `/api/v1/surveys/{surveyId}` | `updateSurvey` | Update a survey. |

## Users

| Method | Path | Handler | Description |
| --- | --- | --- | --- |
| GET | `/api/users` | `getAllUsers` | List all users. |
| POST | `/api/users` | `createUser` | Create a user. |
| POST | `/api/users/import` | `importUsersFromExcel` | Import users from a multipart Excel file. |
| GET | `/api/users/search` | `searchUsers` | Search and page through users. |
| POST | `/api/users/sync/ad` | `syncUsersFromActiveDirectory` | Synchronize users from Active Directory. |
| GET | `/api/users/template` | `downloadUserImportTemplate` | Download the user import template. |
| DELETE | `/api/users/{userId}` | `deleteUser` | Delete a user. |
| GET | `/api/users/{userId}` | `getUserById` | Get a user. |
| PUT | `/api/users/{userId}` | `updateUser` | Update a user. |
