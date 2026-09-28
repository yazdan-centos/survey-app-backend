# Question search

Both search endpoints require `ROLE_ADMIN` or `ROLE_SURVEY_ADMIN`. Unauthenticated requests return HTTP 401; other roles return HTTP 403.

`GET /api/questions/search` returns a page of `QuestionResponseDto` objects, including criterion, dimension, and all levels.

`POST /api/questions/search` accepts the same search fields as a JSON object in the request body. Pagination and sorting are supplied as URL query parameters and bound to `Pageable`. Both endpoints call `QuestionService.search(QuestionSearchRequest filter, Pageable pageable)` and return `Page<QuestionResponseDto>` in the response body.

```http
POST /api/questions/search?page=0&size=10&sort=code,asc
Content-Type: application/json

{
  "text": "leadership",
  "role": "BOARD",
  "dimensionKey": "management",
  "description": "excellent"
}
```

Send `{}` to search without filters. The JSON body is required for POST. Page numbers start at zero; the returned `content` contains the requested page and `totalElements`/`totalPages` describe all matching questions.

All filters are optional and are combined with AND. String filters use trimmed, case-insensitive substring matching; `%`, `_`, and `\` are literal characters. Blank strings are ignored.

| Parameters | Meaning |
| --- | --- |
| `q` | OR search across code, question text, survey title/version, criterion name, dimension key/label, and level descriptions |
| `id`, `code`, `text`, `role`, `displayOrder` | Question fields; IDs, role, and numbers match exactly |
| `surveyId`, `surveyTitle`, `surveyVersion`, `surveyActive` | Survey filters |
| `criterionId`, `criterionName` | Criterion filters |
| `dimensionId`, `dimensionKey`, `dimensionLabel`, `dimensionDisplayOrder` | Dimension reached through the criterion |
| `levelId`, `levelNumber`, `description`, `levelTitle`, `levelScore` | At least one level must satisfy all supplied level filters together |
| `createdAtFrom`, `createdAtTo`, `updatedAtFrom`, `updatedAtTo` | Inclusive question timestamp bounds, using ISO-8601 instants such as `2026-01-01T00:00:00Z` |

Question wording is stored in `text`; `description` searches `QuestionLevel.description`. `levelTitle` and `levelScore` correspond to the numeric fields currently stored on `QuestionLevel`. Roles are `MANAGERS`, `BOARD`, `CUSTOMERS`, and `SUPPLIERS`.

Pagination uses zero-based `page` (default 0), `size` (default 20, capped at 200), and repeatable `sort=property,asc|desc`. The default sort is `displayOrder,asc`, with `id` appended to keep pagination stable. Matching multiple levels never duplicates a question or inflates totals.

Sortable fields: `id`, `code`, `text`, `role`, `displayOrder`, `createdAt`, `updatedAt`, `survey.id`, `survey.title`, `survey.version`, `survey.active`, `criterion.id`, `criterion.name`, `criterion.dimension.id`, `criterion.dimension.key`, `criterion.dimension.label`, and `criterion.dimension.displayOrder`.

Invalid typed filter values, unsupported sort fields, and reversed timestamp ranges return HTTP 400.

Example:

```http
GET /api/questions/search?text=leadership&dimensionKey=management&role=BOARD&description=excellent&page=0&size=20&sort=code,asc
```

Page responses include `content`, `totalElements`, `totalPages`, `number`, and `size`. An unmatched search returns an empty `content` array.
