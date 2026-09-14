# Application Features

This document describes the survey backend based on the current source code. API implementations and data-model support are listed separately because some workflows are incomplete. Runtime behavior has not been verified for this documentation change.

## Authentication and access

- Authenticate local database users or LDAP users with a username and password.
- Issue signed JWT bearer tokens containing the username, authorities, and expiration.
- Authenticate subsequent requests through the `Authorization: Bearer <token>` header.
- Use stateless sessions; all endpoints except login require authentication.
- Define application roles: `ADMIN`, `SURVEY_ADMIN`, and `USER`. Endpoint-specific role restrictions are not currently configured.
- Hash local-user passwords with BCrypt and use them for login before falling back to LDAP.
- Create a local administrator at startup when it does not already exist. Defaults are `admin` / `admin`, configurable with `ADMIN_USERNAME` and `ADMIN_PASSWORD`.

## Survey management

- Create surveys with a title, unique version, and active flag.
- Default new surveys to active when the flag is omitted.
- List all surveys, including inactive surveys.
- Associate questions with a survey.

## Question management

- Provide endpoints to create questions and list questions for a survey.
- Accept a question code, text, audience role, and optional answer levels.
- Define four survey audiences: `MANAGERS`, `BOARD`, `CUSTOMERS`, and `SUPPLIERS`.
- Enforce a database uniqueness constraint on survey, audience role, and question code.
- Model question dimensions and display order, with answer levels ordered by level number.

Question creation has mapping gaps described under Current limitations below.

## Excel question import and export

- Accept `.xlsx` uploads for an existing survey using multipart form data.
- Read the first worksheet, skipping the first row as a header.
- Group imported rows by question code and audience role within the uploaded file, attaching each row as an answer level.
- Export survey questions as a downloadable `questions.xlsx` workbook.

The importer reads these columns by position; the exporter writes these headers:

| Column | Header | Meaning |
| --- | --- | --- |
| 1 | `code` | Question identifier within a survey and audience |
| 2 | `text` | Question text |
| 3 | `role` | Exact survey-role enum value, such as `MANAGERS` |
| 4 | `level_title` | Numeric level title in the current importer |
| 5 | `level_score` | Integer level score |
| 6 | `level_order` | Accepted by the importer but not persisted |

Import and export use a different level-field mapping from direct question creation. See Current limitations before relying on this workflow.

## User management and directory synchronization

- Provide a local-user creation endpoint with duplicate username and email checks.
- Default the application role to `USER` and enable newly created users.
- Synchronize users from Active Directory through an authenticated API request.
- Configure directory search base, search filter, and attribute mappings.
- Match directory users to local records by username.
- Create missing users and update existing names, display names, email addresses, employee IDs, departments, and distinguished names.
- Mark synchronized users as LDAP users and clear their deleted flag.
- Return total-read, created, updated, and skipped counts. Unchanged records and records without usernames are counted as skipped.

Synchronization is request-driven; no scheduled synchronization is present. The directory reader currently sets every returned user's enabled flag to true and does not interpret directory account-disable flags or remove users absent from the search results.

## Response data model

The following entities exist, but no response-submission or response-reporting API is implemented:

- Survey responses with an audience role, optional respondent username, and submission timestamp.
- Question answers containing a selected level and a skipped flag, unique per response and question.
- Demographic answers stored as field-key/value pairs linked to a response.
- Dimensions with a unique key, label, display order, and associated questions.

The audience enum allows skipping for board members, customers, and suppliers, and disallows it for managers. There is no submission workflow enforcing this rule yet.

## API endpoints

All endpoints below except login require authentication.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Authenticate locally or against LDAP and obtain a bearer token |
| `POST` | `/api/users` | Create a local user record |
| `POST` | `/api/users/sync/ad` | Synchronize users from Active Directory |
| `POST` | `/api/surveys` | Create a survey |
| `GET` | `/api/surveys` | List all surveys |
| `POST` | `/api/questions` | Create a question with optional levels |
| `GET` | `/api/questions/survey/{surveyId}` | List questions for a survey |
| `POST` | `/api/questions/import` | Upload an Excel file using `surveyId` and `file` parameters |
| `GET` | `/api/questions/export` | Download questions using the `surveyId` query parameter |

## Technical foundation

- Java 17 and Spring Boot, built with Maven and the included Maven wrapper.
- Spring MVC REST controllers and Jakarta Bean Validation on request DTOs.
- Spring Data JPA persistence with the PostgreSQL driver.
- UUID identifiers and automatic creation/update timestamps inherited from `BaseEntity`.
- Spring Security, LDAP authentication, and JWT signing and verification.
- Apache POI for Excel processing.
- Dockerfile and Docker Compose configuration included in the workspace.
- Existing tests cover survey, question, user, and directory-sync services, plus an application-context test. These tests were not run for this document.

## Current limitations

- Direct question creation sets `criterion` from request text but leaves required `text` and `dimension` fields unset. Excel import sets `text` but leaves required `criterion` and `dimension` fields unset. These paths may fail database constraints.
- Direct question creation maps level title and score to `description` and `levelNumber`; Excel import/export uses the separate numeric `title` and `score` fields. Import leaves the required level description unset.
- `QuestionLevel.setLevelOrder` does nothing, and `getLevelOrder` always returns zero, so level order does not round-trip through Excel.
- Local-user creation leaves the non-null `deleted` and `ldapUser` flags unset, which may prevent persistence.
- No update/delete endpoints, survey activation endpoint, dimension-management API, response-submission API, analytics, or reporting API are present.
- Role values exist in the model, but the security configuration currently requires only authentication for protected endpoints.

## Source reference

Implementation files are under `src/main/java/org/mapnaom/surveyappbackend/`: `controller/` defines the survey, question, user, and synchronization endpoints; `auth/` and `security/` define authentication; `service/` contains business logic; and `entity/` defines the data model. Build dependencies are declared in `pom.xml`.
