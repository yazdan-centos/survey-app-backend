# Survey assignment read API

`GET /api/survey-assignments?surveyId=42`

Requires the existing authenticated ADMIN or SURVEY_ADMIN role. The surveyId query
parameter is required and is a numeric database ID. Returns a JSON array of
SurveyAssignmentResponseDto, the same DTO used by POST and DELETE.
Each row includes the assignment id, user details, survey summary, status,
assignedAt, activeFrom, activeUntil, completedAt, revokedAt, createdAt and updatedAt.
UserResponse excludes passwords and association collections.

- 200 with []: survey exists and has no assignments.
- 400: missing or malformed surveyId.
- 401/403: missing authentication or insufficient privileges.
- 404: survey does not exist.

Rows are sorted by assignedAt descending, then id descending. This is an admin
history endpoint: inactive surveys, expired/future windows, completed/revoked
assignments, and disabled/soft-deleted users remain visible. User profile access
rules are unchanged. The existing users picker still excludes deleted users.

The repository fetches user and survey via EntityGraph. DTO mapping runs inside
a read-only service transaction; the response does not serialize JPA entities or
require Open Session in View. No schema change or global EAGER association is needed.

The SPA reads this endpoint on panel mount and retry, cancels stale reads, displays
errors separately from an empty result, and merges POST results by assignment id.
Deploy the backend endpoint before the frontend change.
