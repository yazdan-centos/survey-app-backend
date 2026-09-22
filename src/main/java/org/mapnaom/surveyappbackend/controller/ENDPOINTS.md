# Controller endpoints

Endpoints are grouped alphabetically by entity, then sorted by path and HTTP method. This inventory covers the `controller` package.

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
