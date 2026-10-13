# Feature 001 — Planta Authorization Tasks

Status: complete — verification and independent approval confirmed by the user. See the completion record below. T01–T15 retain their historical requirements; their future-tense wording does not mean implementation is pending and does not authorize new execution.

## Historical execution rules and file references

Follow the numbered tasks in order. Obtain approval of the specification, plan and implementation scope before T01. Preparing tests is distinct from executing them. **Before running any integration test, or any Maven command that may start a Spring context, obtain explicit human approval covering the command and reviewed isolation configuration (T12).** This includes IDE test execution and the focused HTTP suite, `mvn test`, and `mvn verify`. General implementation approval is not execution approval. If the approved isolation configuration or command scope changes, request approval for the changed scope before executing it.

At the original planning stage, only `tasks.md` was created. The write targets below describe future authorized work. All paths are relative to `gestion_be` except the explicitly marked workspace instruction file. File keys below resolve to exact paths wherever used in a task.

| Key | Exact file |
| --- | --- |
| SPEC | `specs/001-planta-authorization/spec.md` |
| PLAN | `specs/001-planta-authorization/plan.md` |
| TASKS | `specs/001-planta-authorization/tasks.md` |
| ROOT-RULES | `../AGENTS.md` (workspace instructions) |
| BE-RULES | `AGENTS.md` |
| CONTROLLER | `src/main/java/com/vortise/gestion/infrastructure/web/PlantaController.java` |
| UNIT | `src/test/java/com/vortise/gestion/infrastructure/web/PlantaControllerTest.java` |
| HTTP | `src/test/java/com/vortise/gestion/infrastructure/web/PlantaAuthorizationHttpTest.java` |
| PROFILE | `src/test/resources/application-planta-authorization-test.yml` |
| POM | `pom.xml` |
| APP | `src/main/resources/application.yml` |
| SCHEMA | `src/main/resources/schema.sql` |
| SEED | `src/main/java/com/vortise/gestion/infrastructure/config/DataSeeder.java` |
| USER-SEED | `src/main/java/com/vortise/gestion/infrastructure/config/UsuarioDemoSeeder.java` |
| EXISTING-TEST | `src/test/java/com/vortise/gestion/infrastructure/config/UsuarioDemoSeederTest.java` |
| SECURITY | `src/main/java/com/vortise/gestion/infrastructure/config/SecurityConfig.java` |
| CORS | `src/main/java/com/vortise/gestion/infrastructure/config/CorsConfig.java` |
| FILTER | `src/main/java/com/vortise/gestion/infrastructure/security/JwtAuthenticationFilter.java` |
| TENANT | `src/main/java/com/vortise/gestion/infrastructure/security/TenantAccessInterceptor.java` |
| CURRENT-TENANT | `src/main/java/com/vortise/gestion/infrastructure/security/CurrentTenantService.java` |
| JWT | `src/main/java/com/vortise/gestion/application/usecase/JwtService.java` |
| ERRORS | `src/main/java/com/vortise/gestion/infrastructure/web/GlobalExceptionHandler.java` |
| ERROR-DTO | `src/main/java/com/vortise/gestion/application/dto/ErrorResponseDto.java` |
| DTO | `src/main/java/com/vortise/gestion/application/dto/PlantaDto.java` |
| PLANTA | `src/main/java/com/vortise/gestion/domain/model/Planta.java` |
| PROYECTO | `src/main/java/com/vortise/gestion/domain/model/Proyecto.java` |
| EMPRESA | `src/main/java/com/vortise/gestion/domain/model/Empresa.java` |
| ROLES | `src/main/java/com/vortise/gestion/domain/model/RolEmpresa.java` |
| REPO | `src/main/java/com/vortise/gestion/domain/repository/PlantaRepository.java` |
| JPA-REPO | `src/main/java/com/vortise/gestion/infrastructure/persistence/JpaPlantaRepository.java` |

Future production edits are limited to CONTROLLER; test edits are limited to UNIT, HTTP and PROFILE. Other listed files are read-only references. Do not introduce frontend changes, new dependencies, repository methods, role restrictions, `SUPER_ADMIN`, global error changes, schema changes or persistent database operations. Record future task results and approvals in the implementation report/conversation; no extra report file is required by this checklist.

## 1. Test isolation and safety

### T01 — Define the isolated test profile

- **Objective:** Prevent the planned HTTP suite from inheriting production initialization settings.
- **Exact files:** Create PROFILE; read APP, SCHEMA, POM, SPEC, PLAN, ROOT-RULES and BE-RULES.
- **Dependencies:** Human approval of the specification, plan and implementation scope.
- **Acceptance criteria:** PROFILE sets the H2 driver and test-only connection credentials, `spring.sql.init.mode=never`, `spring.h2.console.enabled=false`, `spring.jpa.hibernate.ddl-auto=create-drop`, and `spring.jpa.show-sql=false`. The profile is only safe when paired with T02's pre-startup datasource override; it must not be launched by itself. Production configuration remains unchanged.
- **Verification steps:** Statically compare the YAML with PLAN §7 and check property nesting. Confirm neither a production file URL nor production SQL initialization is introduced. Do not launch Spring or Maven.
- **Completion criteria:** Profile is written and statically reviewed; the mandatory T02 pairing is explicit.

### T02 — Install pre-startup isolation and suppress seeders

- **Objective:** Make the HTTP test context self-contained before any database initialization.
- **Exact files:** Create HTTP; read PROFILE, SEED, USER-SEED, JWT, POM and EXISTING-TEST.
- **Dependencies:** T01.
- **Acceptance criteria:** HTTP uses `@SpringBootTest`, `@AutoConfigureMockMvc` with filters enabled and `@ActiveProfiles("planta-authorization-test")`. `@DynamicPropertySource` registers a once-generated UUID URL `jdbc:h2:mem:planta_auth_<UUID>;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, a generated test-only HMAC signing key and test expiration before context startup. Both startup seeders are replaced with no-op `@MockBean` instances. A JDBC metadata guard checks the generated in-memory database identity before fixture writes. Context teardown is defined. No file database, production secret, `AUTO_SERVER`, reset endpoint or production schema script is used.
- **Verification steps:** Read the context annotations, dynamic properties and seeder replacements; trace initialization order and ensure the runtime URL assertion is supplementary, not the only safety measure. Review without executing the suite.
- **Completion criteria:** Test context skeleton and isolation guards are ready for later fixtures; no context has been started.

## 2. Ownership validation

### T03 — Validate ownership before PUT mutation

- **Objective:** Enforce BR-01 and BR-05 before modifying an entity.
- **Exact files:** Modify CONTROLLER; read REPO, JPA-REPO, PLANTA, DTO, ERRORS and ERROR-DTO.
- **Dependencies:** T02; implementation approval remains in effect.
- **Acceptance criteria:** Add private `requirePlantaInProyecto(Long proyectoId, Long id)`. Missing child, null parent, null parent ID and mismatched parent ID all throw `IllegalArgumentException("Planta no encontrada")`. Compare identifier values. `update(Long, Long, PlantaDto)` calls the helper before any setter, changes only the six existing mutable fields, preserves ID/parent and ignores body identity fields. Existing 200 response and numeric-null semantics remain. Covers AC-01, AC-03, AC-05, AC-07 and BR-04/BR-05.
- **Verification steps:** Inspect the method ordering and every mutation site. Confirm the helper contains no role bypass and no global error or repository change. Runtime verification is deferred to T11/T13.
- **Completion criteria:** PUT uses the shared ownership guard and the diff is limited to the proposed controller behavior.

### T04 — Validate ownership before DELETE

- **Objective:** Apply the same check to deletion and make missing-child handling consistent.
- **Exact files:** Modify CONTROLLER; read REPO and ERRORS.
- **Dependencies:** T03.
- **Acceptance criteria:** `delete(Long, Long)` invokes the helper before `deleteById`, deletes only the verified entity ID and returns 204/empty body after success. Missing/mismatched children use the same 404 mapping as PUT. GET/POST, tenant interception and role permissions remain unchanged. Covers AC-02, AC-04, AC-05 and AC-07.
- **Verification steps:** Statically trace lookup → ownership → delete; check that no denial branch reaches deletion and no catch masks persistence failures.
- **Completion criteria:** Both mutation methods share one guard; no unguarded mutation remains in either method.

## 3. Unit tests

### T05 — Cover valid operations and resource identity

- **Objective:** Verify the supported successful behavior without Spring or a database.
- **Exact files:** Create UNIT; read CONTROLLER, DTO, PLANTA and PROYECTO.
- **Dependencies:** T04.
- **Acceptance criteria:** Add `updateOwnedPlantaPreservesIdentityAndPersistsSupportedFields`, `deleteOwnedPlantaDeletesOnlyVerifiedId` and authorized cases of `updateIgnoresBodyIdentity`. Use JUnit/Mockito and existing repository mocks. Capture the saved object and assert all six fields, original ID and parent; check null numeric values and null/conflicting body IDs. Assert lookup precedes the single correct delete and no save occurs on delete. Use equal parent IDs with distinct boxed instances outside the cache to expose reference comparisons. Covers AC-01/AC-02 and BR-05.
- **Verification steps:** Review assertions against DTO fields and returned responses; ensure there are no context-loading annotations or database access. Execute only in T11.
- **Completion criteria:** Successful operations and identity behavior have concrete unit test cases ready to run.

### T06 — Cover denials and absence of mutations

- **Objective:** Prove rejection occurs before setters and repository writes.
- **Exact files:** Modify UNIT; read CONTROLLER, PLANTA and PROYECTO.
- **Dependencies:** T05.
- **Acceptance criteria:** Parameterize `updateRejectsMissingOrMismatchedPlanta` and `deleteRejectsMissingOrMismatchedPlanta` over missing child, same-company other project, other-company project, null parent and null parent ID. Add wrong-parent requests with forged body IDs. Each denial asserts identical exception/message, no `save`, no `deleteById`, and unchanged scalar fields/identity/association where an entity exists. Covers AC-03–AC-05, AC-07 and BR-04/BR-05. Do not claim these direct calls cover MVC validation or tenant/authentication policy.
- **Verification steps:** Inspect parameter coverage and pre/post snapshots; confirm both methods are covered and tests would detect a setter before the guard. Execute only in T11.
- **Completion criteria:** Every ownership-denial branch has meaningful non-mutation assertions.

## 4. HTTP integration tests

Writing the following tests does not authorize executing them. All executions depend on T12.

### T07 — Add committed fixtures and persistence observations

- **Objective:** Provide realistic fixtures and detect persisted side effects.
- **Exact files:** Modify HTTP; read PLANTA, PROYECTO, EMPRESA, REPO and JPA-REPO.
- **Dependencies:** T02 and T06.
- **Acceptance criteria:** Create Company A/Projects A1 and A2, Company B/Project B1, plants in each, a sibling plant in A1, and a company-less project. Use generated IDs and a verified missing ID; set project company associations explicitly. Commit setup before requests. Observe real repository calls through spies without stubbing persistence; clear observation history after setup/snapshot reads. Read post-request scalar snapshots in a fresh persistence context and compare all fixture plants, parent/company associations and counts. Do not wrap the whole test in a rollback transaction. Cleanup runs child-before-parent only in the verified ephemeral database.
- **Verification steps:** Statically inspect transaction boundaries, isolation guard placement, spy clearing and cleanup order; ensure snapshots avoid entity `equals`/`toString`. Execute only in T13.
- **Completion criteria:** Fixture and assertion helpers can distinguish an actual write from an unchanged database without relying on rollback.

### T08 — Cover valid ownership, wrong parents and missing children

- **Objective:** Test endpoint ownership and response contracts through real MVC/security/JPA.
- **Exact files:** Modify HTTP; read CONTROLLER, ROLES, ERRORS and ERROR-DTO.
- **Dependencies:** T07.
- **Acceptance criteria:** For PUT and DELETE, cover correct ownership for every existing role; same-company wrong-parent requests; foreign-child requests through an accessible project; missing children; and repeated delete (204 then 404). Assert PUT 200/all fields/original IDs and DELETE 204/empty body/only target deleted. All wrong-parent/missing-child requests return identical generic 404 bodies with no foreign details, no repository save/delete and unchanged persisted snapshots. Include administrators in mismatch cases. Covers AC-01–AC-05, AC-07 and BR-04.
- **Verification steps:** Review the role/method/scenario matrix and exact error-body assertions; confirm siblings, projects and companies are retained. Execute only in T13.
- **Completion criteria:** Success and child-level denial scenarios are implemented for both endpoints with persistence and interaction assertions.

### T09 — Cover tenant policy and authentication ordering

- **Objective:** Demonstrate preservation of the current security boundary and administrator behavior.
- **Exact files:** Modify HTTP; read SECURITY, CORS, FILTER, TENANT, CURRENT-TENANT, JWT and ROLES.
- **Dependencies:** T08.
- **Acceptance criteria:** Use real JWTs with synthetic claims and the test-only key, real filters/interceptor and real tenant service. Test all non-admin roles against a foreign URL project (403); missing/company-less URL projects for admin and non-admin (403); correct cross-company addressing by an administrator (success); and missing, malformed, expired and wrong-key tokens (existing authentication rejection). Authentication/tenant denial must precede Planta lookup and all mutations. Preserve the currently expected missing-company-claim behavior (409 for a signed non-admin token) as characterization. Pin actual authentication status during T13, without changing production policy to satisfy an assumed 401/403. Covers AC-06, AC-08, AC-10 and BR-02/BR-03.
- **Verification steps:** Trace filter → interceptor → controller ordering and inspect no-lookup/no-write assertions plus unchanged snapshots. Ensure `CONSULTA` remains eligible for correctly addressed writes. Execute only in T13.
- **Completion criteria:** Policy and ordering cases are written; runtime-dependent status characterization is explicitly left for approved execution.

### T10 — Cover validation, body identity and legacy edge cases

- **Objective:** Ensure payloads cannot change resource identity and validation remains compatible.
- **Exact files:** Modify HTTP; read DTO, CONTROLLER and ERRORS.
- **Dependencies:** T09.
- **Acceptance criteria:** Null/empty/whitespace `nivel` produces 400 with no writes or persisted changes for a correctly addressed authorized target. Null numeric fields remain supported. Conflicting/null body IDs preserve original identity on success and cannot rescue wrong-parent requests. Record MVC validation precedence for invalid payload plus wrong-parent combinations without forcing 404. Characterize malformed JSON/path conversion behavior separately from AC-09; do not alter global errors. Covers AC-09 and BR-04/BR-05.
- **Verification steps:** Review payload variants, assertions and separation of specification requirements from legacy characterization. Execute only in T13.
- **Completion criteria:** Validation and identity tests are written and all AC-01–AC-10 have planned executable coverage.

## 5. Verification

### T11 — Review and run the context-free unit tests

- **Objective:** Obtain focused unit evidence without starting Spring.
- **Exact files:** Read UNIT, CONTROLLER, POM, EXISTING-TEST and BE-RULES; corrections, if needed, remain limited to CONTROLLER and UNIT.
- **Dependencies:** T10 and authorization to implement/verify; no execution is authorized by this document alone.
- **Acceptance criteria:** Review the effective intended test selection and Maven lifecycle configuration statically. Confirm the selected unit class uses only mocks and no plugin execution can start an application/context. Then, within authorized verification scope, run from `gestion_be`: `mvn -Dtest=PlantaControllerTest test`. If context startup cannot be ruled out, defer this command to T12 approval. Report actual outcomes rather than predicted success.
- **Verification steps:** Inspect the source and POM before the command; inspect exit status and test report afterward. Fix focused failures and rerun as needed within the same safe scope.
- **Completion criteria:** Unit tests have passed with recorded evidence, or a blocking failure is recorded; an unexecuted or failing suite does not satisfy feature completion.

### T12 — Obtain explicit approval for context-starting verification

- **Objective:** Present a concrete, reviewable safety configuration before requesting execution approval.
- **Exact files:** Read HTTP, PROFILE, APP, SCHEMA, SEED, USER-SEED, POM, UNIT, EXISTING-TEST, SPEC, PLAN and BE-RULES. No edits required.
- **Dependencies:** T01–T10 complete; T11 completed or its execution explicitly deferred here.
- **Acceptance criteria:** Review the final datasource override before startup, profile activation, seed suppression, generated test key, schema-init disabling, ephemeral cleanup and all test contexts included by broad Maven commands. Present the reviewed configuration and exact proposed commands to the human. Request explicit approval for the focused context-starting suite and subsequent full verification. Approval must state its covered scope; silence and general plan approval do not qualify.
- **Verification steps:** Perform static inspection only. Record the human's response in the conversation/report. If only the focused command is approved, full-suite commands remain gated. If isolation cannot be established, fix the test configuration before requesting approval.
- **Completion criteria:** Explicit execution approval is recorded for each intended command scope; otherwise remain pending and execute no dependent command.

### T13 — Execute focused HTTP and unit verification

- **Objective:** Verify the full authorization flow and persisted effects in isolated H2.
- **Exact files:** Read CONTROLLER, UNIT, HTTP, PROFILE and POM; fix failures only in the four approved implementation/test files unless additional scope is approved.
- **Dependencies:** T12 approval covering this command; T11 unit evidence or approved deferred unit execution.
- **Acceptance criteria:** Run from `gestion_be`: `mvn -Dtest=PlantaControllerTest,PlantaAuthorizationHttpTest test`. Confirm real filters, tenant interceptor, JPA and the database guard participate. Resolve test failures, characterize and pin existing authentication status, and verify every acceptance criterion. Do not change security policies, broaden exception handling or point tests at persistent H2 to make tests pass.
- **Verification steps:** Inspect actual test counts, failures, exit status and persisted-state/interaction assertions. Record skipped checks and material legacy findings without printing tokens, secrets or database contents. Reassess T12 approval if isolation or command scope changes.
- **Completion criteria:** Both focused suites pass with evidence; no unexplained failures/skips remain for required acceptance cases.

### T14 — Run full backend verification and prepare the review evidence

- **Objective:** Confirm regression coverage and assemble a reviewable implementation result.
- **Exact files:** Read CONTROLLER, UNIT, HTTP, PROFILE, POM, EXISTING-TEST, SPEC, PLAN, TASKS and BE-RULES. Review any additional test files discovered before broad execution; discovery does not authorize editing them.
- **Dependencies:** T13 passes and T12 approval explicitly covers full-suite commands and all included contexts.
- **Acceptance criteria:** Run `mvn test`, then `mvn verify`, from `gestion_be`. Verify the diff is limited to approved implementation files and intended documentation, with no persistent H2 changes or frontend modifications. Report changed files, commands, test outcomes, failures/skips, AC mapping and remaining risks. Mention the read/check/write race, lazy association verification and intentional missing-child DELETE 404 behavior; do not claim concurrent atomic authorization.
- **Verification steps:** Review command exit codes/reports, Git status/diff and acceptance mapping. If an unreviewed context is discovered before execution, return to T12. Record frontend verification as not applicable.
- **Completion criteria:** Full backend checks pass and evidence is ready for independent review. Failures or unexecuted mandatory checks remain explicit blockers to completion.

## 6. Independent code review

### T15 — Obtain independent review and close findings

- **Objective:** Fulfill the specification's separate implementation review requirement.
- **Exact files:** Independently review CONTROLLER, UNIT, HTTP and PROFILE against SPEC, PLAN, TASKS, ROOT-RULES and BE-RULES; consult REPO, JPA-REPO, DTO, PLANTA, PROYECTO, SECURITY, CORS, FILTER, TENANT, CURRENT-TENANT and ERRORS as read-only context. No new review file is required.
- **Dependencies:** T14 and its verification evidence.
- **Acceptance criteria:** A reviewer other than the implementer assesses guard ordering, identical child 404s, immutable identity, unchanged role/tenant policy, denied-request non-mutation, realistic HTTP coverage and H2 startup safety. Review all AC mappings and the documented concurrency limitation. Resolve blocking findings within approved scope; request approval before expanding scope. Do not present implementer self-review as independent review or contact another person automatically.
- **Verification steps:** Provide the final diff and test evidence for human-arranged independent review. Record findings and disposition. Rerun relevant checks after corrections under the applicable execution approval, and obtain reviewer confirmation on the corrected result.
- **Completion criteria:** Independent review acceptance is recorded, all blocking findings are resolved, required verification is current and passing, and residual risks are documented. Until review occurs, the feature remains awaiting independent review.

## Acceptance coverage index

| Specification criterion | Implementation tasks | Test authoring tasks | Execution/review evidence |
| --- | --- | --- | --- |
| AC-01 — Valid update | T03 | T05, T08 | T11, T13–T15 |
| AC-02 — Valid delete | T04 | T05, T08 | T11, T13–T15 |
| AC-03 — Cross-project update | T03 | T06, T08 | T11, T13–T15 |
| AC-04 — Cross-project delete | T04 | T06, T08 | T11, T13–T15 |
| AC-05 — Cross-company protection | T03–T04 | T06, T08 | T11, T13–T15 |
| AC-06 — Inaccessible project | Preserve tenant flow | T09 | T13–T15 |
| AC-07 — Missing Planta | T03–T04 | T06, T08 | T11, T13–T15 |
| AC-08 — Missing/company-less project | Preserve tenant flow | T09 | T13–T15 |
| AC-09 — Invalid update payload | Preserve validation | T10 | T13–T15 |
| AC-10 — Authentication/admin behavior | No role exemption in T03–T04 | T08–T09 | T13–T15 |
| BR-04/BR-05 — No mutation/identity | T03–T04 | T05–T10 | T11, T13–T15 |
| Isolated database and execution safety | T01–T02 | T07 | T12 approval, T13–T15 |

## Completion record

Feature 001 is complete based on the user's explicit statement: “Feature 001 is verified and independently approved.” Independent review in this conversation returned **APPROVED WITH NON-BLOCKING OBSERVATIONS**, with no blocking implementation defects against spec.md.

| Tasks | Disposition and evidence |
| --- | --- |
| T01–T10 | Implemented; controller, unit/HTTP suites and profile inspected in the independent review, including untracked files. |
| T11, T13 | Existing Surefire summaries record 23 controller tests and 83 HTTP tests, zero failures/errors/skips. These are prior execution artifacts, not newly executed checks. |
| T12 | Closed as part of user-confirmed verified completion. The exact historical execution-approval transcript is not reproduced here; this record does not grant fresh execution permission. |
| T14 | Full verification confirmed by the user. Existing summaries additionally record 1 seeder test (107 total), zero failures/errors/skips. Individual Maven command exit codes are not established by those summaries. |
| T15 | Independent review accepted with non-blocking observations: suite-wide subclass MockMaker impact and documented concurrency limitation. No blocking corrections remained. |

Evidence sources: `target/surefire-reports/com.vortise.gestion.infrastructure.web.PlantaControllerTest.txt`, `target/surefire-reports/com.vortise.gestion.infrastructure.web.PlantaAuthorizationHttpTest.txt`, and `target/surefire-reports/com.vortise.gestion.infrastructure.config.UsuarioDemoSeederTest.txt`, plus the user's completion confirmation and the review verdict in this conversation. Generated reports may be overwritten by later runs.

Historical command scope: `mvn -Dtest=PlantaControllerTest test`; `mvn -Dtest=PlantaControllerTest,PlantaAuthorizationHttpTest test`; `mvn test`; `mvn verify`. These were the planned verification commands. No per-command execution log, exit status, environment record or immutable source snapshot is available in this documentation pass; do not reconstruct them from test counts. The reviewed working tree included the modified PlantaController and untracked tests, profile and MockMaker resource. No commit identity or source/report fingerprint was captured, so current-source correspondence is not independently certified by these summaries.

### Actual acceptance coverage

Method names below belong to PlantaAuthorizationHttpTest unless otherwise stated.

| Criteria | Implemented test methods |
| --- | --- |
| AC-01, AC-02 | `ownedMutationChangesOnlyTarget`; controller unit tests `updateOwnedPlantaPreservesIdentityAndPersistsSupportedFields`, `deleteOwnedPlantaDeletesOnlyVerifiedId` |
| AC-03, AC-04, AC-05 | `missingOrWrongParentNeverMutates` across methods, roles and denial variants |
| AC-06, AC-08 | `tenantDenialPrecedesChildLookup` |
| AC-07 | `missingOrWrongParentNeverMutates`, `repeatedDeleteReturnsNotFoundWithoutFurtherChanges` |
| AC-09 | `invalidNivelReturnsBadRequestWithoutMutation` |
| AC-10 | `authenticationDenialPrecedesChildLookup`, `administratorCanMutateCorrectlyAddressedForeignCompany`, role-parameterized owned/denied cases |
| BR-04, BR-05 | Denial interaction/state assertions; `bodyIdentityCannotOverrideUrlOwnership`; unit `updateIgnoresBodyIdentity`, `forgedBodyIdentityCannotRescueWrongUrlParent` |

Non-blocking observations are retained in plan.md §12: the MockMaker addition and its limitations are documented; concurrency investigation is deferred. Missing-child DELETE 404 is intentional. Frontend verification is not applicable to this backend-only feature.

**Phase 1:** documentation and agent instructions only; no builds/tests executed and no production/test configuration changed during this phase.

**Historical Phase 1 status:** fail-closed H2 safeguards had not yet been implemented. They were subsequently implemented and verified in Phase 2, as recorded below; Feature 001 acceptance criteria remain unchanged.


## Phase 2 implementation status

Implemented under separate user approval; **execution verification complete: 164 tests, zero failures/errors/skips and BUILD SUCCESS**. Independent Phase 2 review was deferred by the user because H2 is provisional pending PostgreSQL migration; this does not constitute independent approval. The profile-scoped main-source static BeanFactoryPostProcessor and pure policy enforce an explicit datasource/JWT contract, strict UUID-v4 H2 memory URL and configuration allowlists. The HTTP suite adds only two dynamic property registrations; its generated values, 107-test baseline coverage, seeder mocks and metadata guard are retained. Profile defaults are invalid, preventing implicit production URL/key inheritance.

New policy tests perform configuration inspection only. New lifecycle tests use minimal contexts with inert datasource/JPA factory sentinels, including profile resource loading and standalone activation without it. A separate minimal Spring test checks dynamic-property ordering. The new suites and existing regression tests have passed; evidence provenance is recorded in tasks.md under Phase 2 verification record. No production behavior outside the test profile, business logic, acceptance criterion, Maven dependency, JWT service or Mockito setting was changed.

Verification sequence completed: pure policy suite (51), lifecycle/property-ordering suites (6), existing regression suites (107), and full `mvn verify` (164). See the Phase 2 verification record in tasks.md for evidence sources. No new execution is authorized by this record. Custom early connection factories and post-validation property mutations are outside the current guard's guarantee; standalone valid configuration does not suppress real seeders. Concurrency investigation remains deferred.


## Phase 2 verification record

Verification evidence recorded from this conversation on 2026-10-13. All successful runs below report zero failures, errors and skips.

| Gate | Command (from gestion_be) | Result | Evidence source |
| --- | --- | --- | --- |
| Gate 1 | `mvn -Dtest=PlantaTestIsolationPolicyTest test` | 51 tests; BUILD SUCCESS | Agent-observed execution, exit code 0; also confirmed by user-supplied output |
| Gate 2 | `mvn -Dtest=PlantaTestIsolationLifecycleTest,PlantaTestIsolationDynamicPropertiesTest test` | 6 tests; BUILD SUCCESS | User-supplied execution output following the proposed command |
| Existing regression | `mvn -Dtest=PlantaControllerTest,PlantaAuthorizationHttpTest,UsuarioDemoSeederTest test` | 107 tests; BUILD SUCCESS | User-supplied execution output following the proposed command |
| Full backend verification | `mvn verify` | 164 tests; JAR creation/repackaging; BUILD SUCCESS | User-supplied execution output following the proposed command |

Initial Gate 1 execution compiled production and test sources but failed one of 50 cases: the simulated environment source was named `override`, which did not select Boot 3.2's environment-variable mapper. With user approval, only the policy test was corrected to cover `systemEnvironment` and `override-systemEnvironment` and to assert Binder visibility before rejection. The policy was unchanged; the subsequent 51-case run passed.

The lifecycle rejection warnings are expected negative-test behavior. The HTTP run showed JPA and Hikari shutdown. This documentation update did not rerun commands; user-supplied output is attributed rather than presented as agent-observed execution. No immutable source/report fingerprint or commit identity is attached to the user's runs.

**Disposition:** Phase 2 is implemented and verified. Its independent review is deferred by explicit user decision because H2 is provisional and PostgreSQL is planned. Do not label Phase 2 independently approved. Feature 001's earlier independent approval remains unchanged. Retain current H2 safeguards; PostgreSQL migration must establish its own test isolation. Concurrency investigation remains deferred.
