# Backend testing checklist

Use this checklist when planning and reviewing tests. It does not authorize execution. Follow the workspace and backend `AGENTS.md` approval boundaries.

## Selection and isolation

- [ ] Inspect Maven lifecycle configuration and every selected Spring context; `mvn test` currently includes the HTTP suite.
- [ ] Record approval for commands and isolation configuration once; reuse it for retries within scope.
- [ ] Install a generated, per-context in-memory H2 URL and test-only signing key before context startup; never inherit the production file URL or use `AUTO_SERVER`.
- [ ] Disable SQL script initialization, H2 console and SQL logging; use schema creation/teardown only in the isolated database.
- [ ] Suppress both startup seeders before their runners execute. Review newly added initialization hooks.
- [ ] Check JDBC metadata against the exact generated database identity before fixture writes and cleanup. This is supplementary: a post-startup check cannot protect against startup DDL.
- [ ] Commit fixtures before HTTP requests; avoid a test-wide rollback transaction. Observe post-request state through a fresh persistence context.
- [ ] Clean up child-before-parent only in the verified ephemeral database and close the context. Do not enable parallel cases sharing bulk cleanup and fixtures without a separate isolation design.

## Multi-tenant mutation coverage

- [ ] Exercise real JWT authentication, filters, tenant interceptor, MVC validation and JPA. Use repository spies for observation, not stubbed authorization or persistence.
- [ ] Cover every mutation method and existing role, including correctly addressed administrator access across companies and existing CONSULTA behavior.
- [ ] Distinguish an inaccessible URL project (403) from an accessible project with a wrong child (404); include same-company and cross-company mismatches.
- [ ] Cover absent children, missing/company-less projects, repeated deletion and generic identical child-denial responses without foreign details.
- [ ] Cover missing, malformed, expired and wrong-key tokens; assert denial before child lookup where applicable.
- [ ] Cover invalid payloads, nullable supported fields and conflicting/null body IDs. Assert identity and parent preservation.
- [ ] On denial, assert no repository writes/deletes AND unchanged scalar state. A missing save call alone does not exclude dirty checking.
- [ ] Compare committed target, sibling and parent/company associations and row counts. Clear spy history after fixture setup and snapshot reads.
- [ ] Keep legacy status/validation-precedence characterization distinct from desired authorization policy; do not change policy to satisfy an assumed status.

Feature 001's controller and HTTP tests are concrete examples. Extract shared fixture code only when another suite demonstrates a need.

## Mockito subclass MockMaker

`src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` selects `mock-maker-subclass` for the entire test classpath, not only Feature 001. It cannot mock final classes or intercept final methods; static and constructor mocking require another supported mechanism. Use real records/DTOs as test values. Do not remove `final` or weaken production design merely to accommodate mocks.

Retain the current choice unless a demonstrated requirement justifies changing it. Its original adoption rationale was not established in the available review evidence; do not invent an environment diagnosis. A future change requires documented rationale, scope review and authorized full-suite verification.

## Evidence and closure

- [ ] Record commands, exit outcomes, counts, failures/skips, environment and reviewed revision or working-tree snapshot, including untracked implementation files.
- [ ] Distinguish observed execution from existing reports and user-confirmed completion. Surefire summaries alone do not establish `mvn verify` success or current-source correspondence.
- [ ] Map acceptance criteria to actual test methods, record independent review and disposition, and document residual risks.
- [ ] H2 coverage does not establish concurrency/locking behavior on another database engine.

## Phase 2 safeguards — IMPLEMENTED AND VERIFIED

The profile now references explicit `app.test-isolation.datasource-url` and `app.test-isolation.jwt-secret` properties, with invalid defaults. Main-source `PlantaTestIsolationConfiguration`, active only for this profile, registers a static BeanFactoryPostProcessor that validates the effective configuration before ordinary DataSource/JPA singleton creation. It also applies when the profile is activated without test resources.

The policy accepts only the exact UUID-v4 memory URL and two existing H2 options, requires matching effective URL/key settings and a canonical Base64 64-byte key, and rejects alternative datasource/JPA property routes. This intentionally narrow allowlist may reject otherwise benign pool tuning. It does not prove key entropy or protect against custom code opening connections during bean-definition processing or changing properties after validation.

The existing HTTP suite retains generated URL/key values, seeder mocks and JDBC metadata guards. Standalone startup with a valid contract still runs real seeders against the accepted memory database; missing contract must fail. Verification completed with 164 tests, zero failures/errors/skips and `BUILD SUCCESS`, as shown in the user-supplied full verification log. Lifecycle negatives use minimal contexts and inert factory sentinels, never real datasource/JPA auto-configuration. See [the verification record](../specs/001-planta-authorization/tasks.md#phase-2-verification-record). Independent Phase 2 review was deferred by user decision because H2 is provisional pending PostgreSQL migration; no independent approval is claimed. Keep the current protections until a separately reviewed replacement is available.
