# SFO survey initialization

`SfoDataInitializer` runs after the World Class initializer and reads
`static/sfo_questions.json`. SFO is the spelling used in the supplied resource.

The initializer creates survey version `sfo-1.0.0`, five namespaced dimensions,
one criterion per principle (using its English subtitle), and 28 manager questions
in source order. Six demographic questions and their ordered options are available
under group key `sfo-managers`; this uses the existing demographic group API rather
than a survey foreign key. Source demographic IDs d1-d6 correspond to display
orders 0-5. Existing records are preserved and missing questions are restored on
subsequent starts. Initialization runs in one transaction.

The source does not define assessment answer options or scores. Consequently no
levels are invented, and the survey is initially inactive. Configure its question
levels before activating it. An administrator's later activation is preserved.

Question text is mapped as TEXT to retain long Persian prompts. For an existing
database with Hibernate schema updates disabled, apply
`src/main/resources/db/sfo_question_text.sql` before starting the application.
The script is manual, not an automatically registered Flyway migration.
The application's current `ddl-auto=create` configuration recreates data on startup;
use `update` for an existing development database when records must survive restarts.
Run seed initialization on one instance at a time; concurrent seeders are not coordinated.
