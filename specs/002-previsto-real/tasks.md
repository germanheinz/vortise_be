# Feature 002 — implementation and verification

## Implemented
- [x] Reference equations and thresholds from PREV VS REAL.xlsx; golden fixtures for
  both daily loads and their activity aggregate (no averaging ratios).
- [x] Server-side HH derivation from official/helper HH per unit.
- [x] Nullable task/activity foreign key; explicit project ownership validation.
- [x] Stable activity IDs on project edits; reject deletion/unit changes with linked tasks.
- [x] Require destination company access when updating project company.
- [x] Reuse enabled OFICIAL/AYUDANTE company hourly costs and weekday jornada.
- [x] Read-only indicators API with per-load and per-activity totals.
- [x] Tasks show the five indicators and refresh after create/edit/delete.
- [x] Expected daily quantity and compliance derive from planned yield and actual HH.
- [x] Schedule actuals use tasks only, joined by activity ID; actual month uses task date.
- [x] Yield editor and schedule save; explicit errors/loading/missing-data states.
- [x] Remove synthetic productivity curve/month markers and unrelated loss estimate
  from this schedule; retain actual monthly blue/gray comparison and hour totals.
- [x] Pure formula, aggregation, identity, ownership and frontend adapter tests.
- [x] Isolated HTTP/JPA test class prepared and compiled, not executed yet.

## Verification evidence — 2026-10-14
Executed by the agent:
- BE: `mvn clean -Dtest=PrevistoRealCalculatorTest,PrevistoRealUseCaseTest,RubroPlanningTest test`
  compiled 194 main and 12 test source files with javac; **18 tests, 0 failures/errors/skips,
  BUILD SUCCESS**. Clean removed generated target output only. An earlier added fixture
  used a nonexistent Empresa setter; corrected to a mock, then clean compilation verified.
- FE: `./node_modules/.bin/tsc --noEmit --incremental false` — exit 0.
- FE: `node --test tests/*.test.cjs` — **11 passed**, no failures.
- FE: `npm run build` — exit 0; Next production build completed.
- Both repositories: `git diff --check` — passed.

Pending approval (asked in chat): `mvn -Dtest=PrevistoRealHttpTest test`.
This new context uses the existing fail-closed profile, UUID memory H2 URL,
64-byte random JWT material, mocked startup seeders, JDBC metadata assertion before
fixture creation, transaction rollback and context destruction. Six tests cover
owned create/read with Excel results, foreign activity rejection, anonymous and
foreign-company reads, stable persisted linkage after plan edits, project cascade
delete, and rejection of moving a plan into an inaccessible company.

- [ ] Execute six HTTP/JPA tests after reviewed approval.
- [ ] Browser verification: configure company rates and jornada, save planned yields,
  record two Excel loads, verify cumulative results, then edit and delete a load.
- [ ] Full regression execution (not claimed by the focused tests above).

## Files changed for this feature
Backend (paths relative to gestion_be):
- src/main/java/com/vortise/gestion/application/dto/{ObraTareaCreateDto,ObraTareaDto,RubroCreateDto}.java
- src/main/java/com/vortise/gestion/application/usecase/{CreateObraTareaUseCase,GetObraTareasUseCase,CreateProyectoUseCase,UpdateProyectoUseCase}.java
- src/main/java/com/vortise/gestion/application/usecase/{PrevistoRealCalculator,GetPrevistoRealUseCase,RubroPlanning}.java (new)
- src/main/java/com/vortise/gestion/domain/model/ObraTarea.java
- src/main/java/com/vortise/gestion/infrastructure/web/ObraTareaController.java
- src/test/java/com/vortise/gestion/application/usecase/{PrevistoRealCalculatorTest,PrevistoRealUseCaseTest,RubroPlanningTest}.java (new)
- src/test/java/com/vortise/gestion/infrastructure/web/PrevistoRealHttpTest.java (new)
- specs/002-previsto-real/{spec,plan,tasks}.md (new)

Frontend (paths relative to gestion_fe):
- types/index.ts; lib/api.ts; lib/cumplimiento.ts
- lib/tareas-cronograma.ts (new)
- app/proyectos/[id]/{tareas,cronograma}/page.tsx
- components/proyectos/{TareasSection,CronogramaSection,CronogramaMensual,EstructuraActividadesSection}.tsx
- components/proyectos/PrevistoRealPanel.tsx (new)
- tests/cumplimiento.test.cjs; tests/tareas-cronograma.test.cjs (new)

Other pre-existing workspace changes (including earlier seeder changes, sidebar,
restrictions, company defaults and database files) were not reset. The subsequent
approved seeder extension is recorded below separately.
No persistent database operations, application startup, dependency installation,
commit, push or deployment were executed.

## Demo limitations / operational notes
- New nullable obra_tareas.rubro_id FK needs schema application at backend startup;
  persistence and schema behavior await the isolated HTTP/JPA gate.
- Existing unlinked tasks require explicit activity selection on edit; only precisely
  recognized untouched demo placeholders are migrated by the seeder extension.
- Old RegistroHoras records remain stored but no longer contribute to this schedule.
- Current company tariffs apply to all comparison loads; historical price snapshots,
  materials, overtime premiums and financial currency conversion are outside this demo.
- Missing or disabled category prices leave money indicators unavailable.
- Resource is saved jornadas, not headcount; positive variance is savings.
- Projection is remaining work only, clamped to zero when activity quantity is complete.
- Excel production thresholds are absolute activity units, retained for demo fidelity.
- Schedule automatic dates retain the existing calendar-day behavior; the Excel's
  five-day workweek is not a new workday/holiday scheduling engine.
- No cross-activity productivity average across different physical units.
- Multi-user optimistic locking/version conflicts remain deferred; edits use the
  existing full-project update API, so concurrent saves can overwrite newer values.

## Demo seeder extension — 2026-10-15
- [x] Fill missing company units, contacts, cause/restriction catalogs, labor categories,
  calendar and recurring fixed costs without replacing configured values.
- [x] Seed six activities with dated weekday production and official/helper hours,
  including savings and overruns; migrate only recognized untouched demo placeholders.
- [x] Keep hour summaries consistent with daily loads, and align legacy restrictions
  and delivery progress where their original fixture values remain unchanged.
- [x] Add activity labor budgets and monthly income certificates when no manual
  financial rows exist; preserve existing rows on repeated startup.
- [x] Protect tenant boundaries, generated-data idempotence and manual edits.

Changed implementation: `src/main/java/com/vortise/gestion/infrastructure/config/DataSeeder.java`
and new `DemoOperationalSeed.java`. New pure tests in the corresponding test package:
`DemoOperationalSeedTest.java` and `DemoSeederCoverageTest.java`.

Executed: `mvn -Dtest=DemoSeederCoverageTest,DemoOperationalSeedTest,DataSeederTest,DemoMonthlyProgressTest test`
— **21 tests, 0 failures/errors/skips, BUILD SUCCESS**. Tests use plain objects and
mock repositories; they do not start Spring, connect to JDBC or populate the workspace DB.
The simulation extends through February 2027 and is labeled in generated load comments.
Seeder persistence/startup and browser results remain unverified; no backend application
startup, persistent data operation or reset was performed by the agent.
