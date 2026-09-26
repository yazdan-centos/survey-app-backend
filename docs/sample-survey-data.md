# Sample World Class submissions

Enable the optional initializer when starting the application:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.seed.survey-responses.enabled=true"
```

Alternatively set `APP_SEED_SURVEY_RESPONSES_ENABLED=true`. The initializer is disabled by default.

After survey and workbook user initialization, it randomly selects five distinct enabled, nondeleted `USER` accounts and creates one complete `MANAGERS` submission per account for survey version `1.0.0`. The manager audience matches the imported manager workbook; security roles do not determine survey audiences. Every manager question receives a randomly selected existing level, with no skipped answers.

Each response includes `department` (the account's department, or a simulated fallback), and simulated `gender`, `ageRange`, `education`, and `yearsOfExperience` fields. These demographic keys are free-form sample conventions, not a frontend schema. No user profile is modified.

The `sampleDataSource=worldclass-1.0.0-five-users` demographic identifies these submissions as synthetic. If this marker already exists, startup leaves submissions unchanged. Existing unmarked submissions are preserved. All five submissions and their children are written in one transaction; missing questions/levels or fewer than five eligible users fail initialization before any submissions are saved.

Run this on a development/demo database: the submissions use existing usernames and count in dashboard aggregates. Enable it on one application instance at a time; concurrent initializers are not coordinated. Keeping at least one marked response prevents reseeding on later starts.
