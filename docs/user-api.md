# User management API

All `/api/users` endpoints require a bearer token with `ROLE_ADMIN`, including the existing create and AD sync endpoints. User JSON responses omit passwords and password hashes.

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/users` | All non-deleted users |
| GET | `/api/users/{id}` | A non-deleted user, or 404 |
| PUT | `/api/users/{id}` | Updated user |
| DELETE | `/api/users/{id}` | Soft delete; 204 with no body |
| GET | `/api/users/search` | Filtered, sorted page of non-deleted users |
| POST | `/api/users/import` | Import `.xlsx` multipart field `file`; `{"createdCount":3}` |
| GET | `/api/users/template` | Download `users-template.xlsx` |

## Update

```json
{
  "username": "alice",
  "firstName": "Alice",
  "lastName": "Example",
  "displayName": "Alice Example",
  "email": "alice@example.com",
  "employeeId": "00123",
  "department": "Engineering",
  "role": "USER",
  "enabled": true
}
```

`username` is required, at most 100 characters. Profile fields are replaced: omitted first/last/display names, email, employee ID and department become null. Email is optional but must be valid when supplied. First name, last name, employee ID and department allow 100 characters; display name and email allow 200.

Omitted or null `role`, `enabled` and `password` preserve their current values. A supplied password must be nonblank and at most 72 UTF-8 bytes; it is encoded before storage. LDAP usernames and passwords cannot be changed through this API. LDAP origin, DN, IDs, timestamps and the deleted flag are managed by the server.

Roles: `ADMIN`, `SURVEY_ADMIN`, `USER`. Duplicate username/email returns 409, invalid input returns 400, and a missing or deleted user returns 404. Service errors include a problem-detail JSON `detail` field.

## Search

```http
GET /api/users/search?q=alice&department=engineering&role=USER&enabled=true&page=0&size=20&sort=username,asc
```

Filters combine with AND. `q` searches username, first name, last name, display name, email, employee ID and department with OR. `username`, `email`, `employeeId` and `department` accept case-insensitive substring matches; `%` and `_` are treated literally. `role`, `enabled` and `ldapUser` are exact filters. Omit filters to page through all non-deleted users.

Pages start at zero. Default size is 20; maximum size is 200. Repeat `sort` for multiple sort fields. Supported fields: `id`, `username`, `firstName`, `lastName`, `displayName`, `email`, `employeeId`, `department`, `role`, `enabled`, `ldapUser`, `createdAt`, `updatedAt`. Default ordering is username ascending; ID is added as a stable tie-breaker. Unsupported sort fields return 400. The response includes `content`, `totalElements`, `totalPages`, `number` and `size`.

## Excel import

Download the template, fill the first worksheet starting on row 2, and upload it as multipart field `file` to `/api/users/import`. The header names are:

```text
username,email,password,firstName,lastName,displayName,employeeId,department,role,enabled
```

Keep every header; columns may be reordered. Username and password are required for each populated row. Other cells are optional. Blank role defaults to `USER`; blank enabled defaults to `true`. Role values are case-insensitive; enabled accepts only `true` or `false`. The template formats columns as text to preserve identifiers such as `00123`. Password spaces are preserved.

Imports create local users (`ldapUser=false`, `deleted=false`). They do not update existing users. Duplicate usernames/emails, including those reserved by deleted users, return 409. Invalid rows return 400 with the Excel row number; no rows are committed when the import fails. Blank rows are skipped, and formulas/error cells are rejected. The importer accepts at most 5,000 data rows and 10 MB; the server's multipart upload limits may impose a smaller limit. Only `.xlsx` is supported.

The template has no sample user rows, so uploading it without adding users returns 400.

## Deletion behavior

Deletion sets `deleted=true` and `enabled=false`; the record and its unique username/email remain stored. Reads, updates and searches exclude deleted users. Repeated deletion returns 404. AD sync skips deleted users instead of restoring them.

The existing authentication system uses stateless JWTs: deletion, password changes and role changes do not revoke already-issued tokens. LDAP authentication remains controlled by the directory; this API does not remove or disable directory accounts.

## Verification

```powershell
.\mvnw.cmd "-Dtest=UserServiceTest,UserManagementTest,UserExcelServiceTest,UserControllerTest,UserSearchTest,UserSyncServiceTest,UserApiSecurityTest" test
```

Search and persistence tests use an isolated H2 database; they do not connect to the application's PostgreSQL database.
