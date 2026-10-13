# Feature 001 — Planta Authorization Implementation Plan

Status: complete — user-confirmed verification and independent approval; see [completion evidence](tasks.md#completion-record). Sections 1–11 preserve the original design and execution plan as historical context, not a pending approval request or fresh execution authorization. Section 12 records deviations and deferred follow-ups.

## 1. Scope and architectural findings

The governing contract is [spec.md](./spec.md), BR-01–BR-05 and AC-01–AC-10. All proposed implementation belongs to `gestion_be`; no frontend or API type changes are required. Feature 002 and `SUPER_ADMIN` are outside scope.

The backend uses Java 17, Spring Boot 3.2.0, Spring MVC, Spring Security, JPA and H2. Domain repositories act as partial ports; Spring Data interfaces extend them. Domain entities contain JPA mappings. Although other features use application use cases, `PlantaController` already accesses `PlantaRepository` and `ProyectoRepository` directly. Preserve that local structure without introducing new controller dependencies or a broader architecture refactor.

Current execution flow:

1. `SecurityConfig.securityFilterChain(HttpSecurity, JwtAuthenticationFilter)` installs stateless authentication and requires authentication for the affected routes. There are no role-specific write restrictions on these endpoints.
2. `JwtAuthenticationFilter.doFilterInternal(...)` parses a bearer token through `JwtService.parse(String)`, installs `ROLE_<rol>` and retains JWT claims as authentication details. Invalid tokens clear authentication.
3. `CorsConfig.corsConfigurer()` registers `TenantAccessInterceptor` for `/api/proyectos/**`.
4. `TenantAccessInterceptor.preHandle(...)` resolves the URL project through `ProyectoRepository.findById`, obtains `empresaRelacionada.id`, then calls `CurrentTenantService.requireEmpresa(Long)`. Missing/company-less projects are denied before controller execution. The `Optional.map` expression also turns a null company into an empty result, so the generic existing denial message can apply.
5. For non-administrators, `CurrentTenantService.empresaId()` reads the company claim and `requireEmpresa` checks equality. `ADMINISTRADOR` currently bypasses company equality; this is an existing policy, not a new permission introduced here.
6. Spring MVC resolves and validates `PlantaDto`. Only `nivel` has a validation constraint (`@NotBlank`).
7. `PlantaController.update(Long proyectoId, Long id, PlantaDto dto)` currently loads by child ID alone, changes six fields and saves. `delete(Long proyectoId, Long id)` currently calls `deleteById(id)` without loading or checking ownership.
8. `JpaPlantaRepository` extends both `JpaRepository<Planta, Long>` and `PlantaRepository`. `Planta.proyecto` is a lazy many-to-one association with a non-null foreign key. `Proyecto.empresaRelacionada` is nullable. `GlobalExceptionHandler` converts exceptions into `ErrorResponseDto(status, error, message)`.

The vulnerability is a missing child-to-parent check after authorization of the URL project. An authorized URL project does not establish ownership of an arbitrary Planta ID.

## 2. Proposed ownership validation

Add one private controller helper: `Planta requirePlantaInProyecto(Long proyectoId, Long id)`.

Its behavior is:

1. Load the child using the existing `plantaRepository.findById(id)`.
2. If absent, throw `IllegalArgumentException("Planta no encontrada")`.
3. Before any setter or repository mutation, reject a null `planta.getProyecto()` or a parent ID unequal to the URL `proyectoId`, using the identical exception and message. Compare identifier values, not entity equality or boxed-Long reference equality. A null persisted parent ID must fail closed.
4. Return the verified entity.

`update` calls the helper before its first setter. Continue assigning only `nivel`, `superficie`, `supCubierta`, `supDescubierta`, `supCalculada`, and `coefPlantaTipo`, then save and map the same entity. Never assign `dto.id()` or `dto.proyectoId()` to the entity; conflicting body IDs remain ignored as today. Preserve null handling for the unconstrained numeric fields.

`delete` calls the same helper and then `deleteById` with the verified entity ID. Return 204 only after that call succeeds. A missing child now returns 404 deterministically at the initial lookup, including an ordinary repeated delete.

Authorization order remains authentication → URL-project tenant check → MVC payload validation (PUT) → child lookup/ownership → mutation. A valid-body request for an inaccessible project must fail before child lookup. A blank `nivel` may yield 400 before child ownership evaluation; do not promise 404 precedence for simultaneously invalid payloads and mismatched children.

## 3. Exact production and test changes after approval

Paths below are relative to `gestion_be`.

| File/class | Planned change |
| --- | --- |
| `src/main/java/com/vortise/gestion/infrastructure/web/PlantaController.java` | Add `requirePlantaInProyecto(Long, Long)`; modify `update(Long, Long, PlantaDto)` and `delete(Long, Long)` as above. |
| `src/test/java/com/vortise/gestion/infrastructure/web/PlantaControllerTest.java` | New JUnit/Mockito tests of ownership, unchanged entities on denial, identity preservation and repository interactions. |
| `src/test/java/com/vortise/gestion/infrastructure/web/PlantaAuthorizationHttpTest.java` | New full-context MockMvc HTTP integration tests with real filters, tenant interceptor, exception advice and JPA repositories; test-only seed suppression and database/JWT overrides. |
| `src/test/resources/application-planta-authorization-test.yml` | New isolated test profile described below. |

No changes are planned to `PlantaRepository`, `JpaPlantaRepository`, DTOs, entities, `GlobalExceptionHandler`, security classes, production configuration, Maven dependencies, seeders, GET/POST endpoints, or the specification. Existing repository methods suffice. No existing Planta use case needs modification.

## 4. HTTP contract and errors

Routes and JSON shape remain unchanged:

- `PUT /api/proyectos/{proyectoId}/plantas/{id}` accepts `PlantaDto` and returns HTTP 200 with `PlantaDto`, including the original ID and parent ID.
- `DELETE /api/proyectos/{proyectoId}/plantas/{id}` returns HTTP 204 with an empty body on success.

| Condition | Result and handling |
| --- | --- |
| Child missing or outside URL project, including another company | 404 via existing `handleIllegalArgument`; JSON `{ "status": 404, "error": "Not Found", "message": "Planta no encontrada" }`. Same response for both cases; no actual owner, company, fields or existence confirmation. |
| URL project inaccessible, missing or company-less | Existing `AccessDeniedException` → `handleAccessDenied` → 403. Preserve current messages and company-access policy. |
| `nivel` null, empty or whitespace | Existing `handleValidation` → 400; no mutation. |
| Missing/invalid/expired authentication | Rejection by the existing security filter chain before mutation. No new authentication entry point or response format. Source configuration suggests the default forbidden response (403); characterize and pin the actual existing status during approved verification rather than change it to 401. |
| Unexpected persistence/runtime failure | Existing handling remains; do not catch broadly and disguise failures as ownership 404. |

Reuse the existing `IllegalArgumentException` mapping deliberately for this focused fix. Do not change the application's global exception taxonomy. Malformed JSON and path conversion failures can reach the broad existing exception handler; characterize those separately without expanding AC-09 beyond existing bean-validation constraints or silently changing unrelated error behavior.

## 5. Tenant and role preservation

Keep the real tenant interceptor active in all HTTP tests. Do not replace it with controller-only authorization or mock `CurrentTenantService` in integration tests.

- `JEFE_DE_OBRA`, `RESPONSABLE` and `CONSULTA` retain their current ability to perform these writes when their company matches the URL project's company. Do not introduce `@PreAuthorize` restrictions.
- `ADMINISTRADOR` retains access to correctly addressed projects across companies, but must pass exactly the same Planta–Proyecto check as every other role.
- Even administrators receive 403 for a missing or company-less URL project because the project resolution precedes the company bypass.
- A non-administrator addressing a foreign URL project receives 403. A user addressing an accessible project with a foreign child receives 404. These are distinct cases.
- Do not add `SUPER_ADMIN`, change JWT claims, reinterpret membership, or change authentication policy.

## 6. Unit and HTTP integration strategy

### Controller unit tests

Use JUnit 5 and Mockito from the existing `spring-boot-starter-test`. Instantiate the controller with mocked existing repositories; no Spring context or database.

Test methods/cases should cover:

- `updateOwnedPlantaPreservesIdentityAndPersistsSupportedFields`: capture saved object and assert all six fields, original ID and original project reference.
- `deleteOwnedPlantaDeletesOnlyVerifiedId`: lookup occurs before exactly one matching delete; no save.
- `updateRejectsMissingOrMismatchedPlanta` and `deleteRejectsMissingOrMismatchedPlanta`: parameterize missing, same-company other project, other-company project, null project and null parent ID.
- `updateIgnoresBodyIdentity`: exercise null and conflicting body IDs with valid and invalid URL ownership; body IDs cannot rescue an invalid URL association.
- Use distinct `Long` instances/values outside the boxing cache to detect accidental `==` comparison.

Every denied controller case asserts the uniform exception/message, `never().save(...)`, `never().deleteById(...)`, and unchanged scalar fields, ID and parent reference. Checking only repository writes is insufficient: setters on a managed entity could otherwise dirty it. Do not claim direct controller tests validate JWTs, tenant interception or `@Valid`.

### HTTP integration tests

Use `@SpringBootTest`, `@AutoConfigureMockMvc` with filters enabled and `@ActiveProfiles("planta-authorization-test")`. MockMvc exercises the HTTP/MVC pipeline in process; a network listener is unnecessary. Use actual `JwtService.generate(...)` with a generated test-only signing key and synthetic subjects/claims. Do not use mocked users without company claims or add `spring-security-test` merely for authentication helpers.

Use real JPA fixtures: Company A with Projects A1/A2; Company B with Project B1; at least one Planta per project plus a sibling in A1; an existing company-less project. Generate database IDs, choose a provably missing ID and do not depend on seed IDs. Set `empresaRelacionada` explicitly when building projects.

Parameterize PUT and DELETE wherever applicable. Exercise every existing role for correct ownership and wrong-parent denial; separately prove administrator cross-company success with a correct parent. Test non-admin foreign URL-project denial and accessible URL-project/foreign-child denial independently.

Use repository spies only to observe actual calls, not to stub ownership or persistence. Reset invocation history after fixture setup and snapshot reads. Denied requests must invoke neither `save` nor `deleteById` on the Planta repository. For authentication/tenant denial, also assert no Planta lookup. Assert status, response shape, identical missing/mismatch 404 bodies and absence of foreign resource details.

Commit fixture setup before requests. Do not wrap the entire HTTP test in a rollback-only transaction that could conceal an actual write. After each response, read state in a fresh transaction/persistence context (or clear the entity manager before reloading). Compare scalar snapshots of every fixture Planta, all parent/company associations and row counts. Denied requests leave the snapshot unchanged. Success updates only the target; success deletes only the target and retains siblings, projects and companies. Avoid entity `equals`/`toString` in snapshots because associations can recurse or trigger lazy loading.

## 7. Isolated H2 configuration and startup safety

Production configuration uses file-backed H2, schema initialization and `ddl-auto: update`; never start these tests using that datasource. Both `DataSeeder` and `UsuarioDemoSeeder` are startup runners with no identified profile gate. A test profile alone does not disable them.

Proposed test profile:

```yaml
spring:
  datasource:
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  sql:
    init:
      mode: never
  h2:
    console:
      enabled: false
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
```

In `PlantaAuthorizationHttpTest`, register a static, once-generated UUID database name through `@DynamicPropertySource`:

`spring.datasource.url = jdbc:h2:mem:planta_auth_<UUID>;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`

Also register `app.security.jwt-secret` from a generated test-only key of sufficient HMAC length, plus a test-only expiration. Do not read or print environment secrets, real tokens, existing credentials or database contents. Explicit dynamic properties must be installed before context creation, rather than changing the datasource after startup. Do not inherit a file URL, use `AUTO_SERVER`, or point cleanup at `data/`.

Replace `DataSeeder` and `UsuarioDemoSeeder` with `@MockBean` instances in this test context so their runner methods are no-ops. This requires no production changes. Verify this setup by code review before running anything. The existing `UsuarioDemoSeederTest` is a pure Mockito test and does not open a database.

As a defense-in-depth assertion before fixture writes, inspect JDBC metadata and fail unless the active URL begins with the exact generated `jdbc:h2:mem:planta_auth_` prefix. This check supplements pre-startup overrides; it cannot by itself protect against an incorrectly configured context startup.

Use test-only transactions to clean fixtures in child-before-parent order after each case, or recreate the isolated context when necessary. Cleanup/schema creation/deletion must touch only this verified ephemeral database. Close the context after the suite (`@DirtiesContext` if needed); `create-drop` and process teardown dispose of test state. Do not run demo-reset endpoints, manipulate tracked H2 files, or execute production `schema.sql` in tests.

## 8. Edge cases and security regressions

- Same-company wrong-parent requests are denied, even though tenant authorization succeeds.
- Cross-company wrong-parent requests and absent IDs have identical child-level 404 responses.
- Valid administrator access across companies remains functional; ownership mismatches still fail.
- Test all three non-admin roles, including `CONSULTA`, to prevent accidental permission tightening.
- Missing/company-less URL projects deny access for admin and non-admin users before child lookup.
- Null/empty/whitespace `nivel` gives 400 with unchanged persisted state. Valid payloads with null numeric fields preserve existing semantics.
- Conflicting `id`/`proyectoId` in PUT bodies never change identity or move a child; test both authorized and denied URL combinations.
- Repeated delete returns 204 then 404. A denied delete cannot remove siblings or either project.
- Missing bearer token, malformed token, expired token and token signed by another test key must all be rejected without child access or writes; characterize existing authentication status rather than impose a new one.
- A signed non-admin token without a company claim currently reaches `IllegalStateException` (409) in `CurrentTenantService`; treat as an existing behavior to characterize without changing authentication/claim policy here.
- Null child-parent associations are a unit-test defensive case because the real foreign key is non-null. Company-less projects are a real integration fixture because that association is nullable.

## 9. Risks, limitations and alternatives

1. **Read/check/write race:** the existing controller has no encompassing transaction or optimistic version. A concurrent delete or reparent through another path can race the check. This focused plan enforces ownership for the loaded state and does not promise atomic behavior across concurrent authorization changes. A plain added transaction alone would not solve this. If atomic concurrent ownership is required, approve a separate design using scoped conditional mutations or suitable locking/versioning, coordinated with every writer.
2. **Lazy associations:** ownership and response mapping read project identifiers from a lazy association. Existing mapping already does this. Real-context tests must confirm behavior with current persistence settings; do not change fetch modes or open-in-view globally for this fix.
3. **Broad exception convention:** `IllegalArgumentException` is globally mapped to 404. Reusing it minimizes impact but is less explicit than a dedicated not-found exception. A global taxonomy change would affect unrelated endpoints and is deferred.
4. **DELETE compatibility:** clients that previously received success for a missing child will now receive 404, intentionally required by AC-07. The lookup adds one read per delete.
5. **Existing administrator bypass:** it remains broader than company-local access. Feature 001 must not silently narrow it; its future redesign belongs to the separate role specification.
6. **Alternative scoped lookup:** `findByIdAndProyectoId` on the domain and JPA repository interfaces would encode parent scope in SQL and avoid loading a foreign child. It adds repository changes, does not by itself eliminate the mutation race, and is unnecessary for the minimal approved behavior. Reconsider if repository-level scoping becomes a project convention.
7. **Alternative use case:** a transactional application use case could centralize mutations for future callers, but adds structure for two methods. The current proposal keeps the existing boundary; direct controller unit calls bypass HTTP tenant authorization by design.
8. **Existing error/path behavior:** malformed route values, unusual encodings and malformed JSON may have inconsistent legacy statuses. Do not broaden this fix into global route/error normalization. Record material findings during later verification for a separate decision.
9. **H2 realism:** H2 verifies the configured persistence path and authorization effects, but does not prove locking behavior on other database engines. No new dependency or migration is justified by this feature.

## 10. Acceptance criteria → test mapping

Test identifiers below describe planned parameterized cases, not already implemented tests.

| Acceptance criterion / rule | Unit coverage | HTTP integration coverage and persisted assertions |
| --- | --- | --- |
| AC-01; BR-01, BR-05 | `updateOwnedPlantaPreservesIdentityAndPersistsSupportedFields`, `updateIgnoresBodyIdentity` | `ownedUpdate`: 200, all supported fields stored, original ID/project, siblings unchanged; all roles. |
| AC-02; BR-01 | `deleteOwnedPlantaDeletesOnlyVerifiedId` | `ownedDelete`: 204/empty body, only target removed; all roles. |
| AC-03; BR-04 | `updateRejectsMissingOrMismatchedPlanta` | `sameCompanyWrongParentUpdate`: 404 and complete unchanged snapshot; include administrator. |
| AC-04; BR-04 | `deleteRejectsMissingOrMismatchedPlanta` | `sameCompanyWrongParentDelete`: 404 and no deletion; include administrator. |
| AC-05; BR-01, BR-04 | Mismatch cases for both methods | `foreignChildThroughAccessibleProject`: PUT/DELETE 404, generic identical response, no foreign details, no writes or snapshot changes. |
| AC-06; BR-02 | Tenant boundary belongs in HTTP tests | `inaccessibleUrlProject`: PUT/DELETE 403 for non-admin roles, no child lookup or writes, unchanged snapshot. |
| AC-07; BR-04 | Missing-child cases for both methods | `missingChild`: PUT/DELETE 404, same body as mismatch, unchanged snapshot; `repeatDelete`: 204 then 404. |
| AC-08; BR-02 | Tenant boundary belongs in HTTP tests | `missingOrCompanylessUrlProject`: PUT/DELETE 403 for admin and non-admin, no child lookup/writes. |
| AC-09; BR-04 | No claim of MVC validation from direct calls | `invalidNivel`: null/empty/blank → 400, no writes and unchanged persisted target. |
| AC-10; BR-03 | Ownership helper contains no role bypass | `unauthenticatedOrInvalidToken`: preserve characterized rejection status, no child access/writes; `administratorCorrectForeignParent`: success; `allRolesWrongParent`: 404. |
| BR-05 supplemental | `updateIgnoresBodyIdentity` | `conflictingBodyIdentity`: original ID/parent preserved on success; forged body cannot bypass wrong-parent denial. |
| Verification requirements | `never` save/delete plus object snapshots on every denied controller case | Real filters/JPA, repository interaction observations, fresh persisted snapshots, verified in-memory datasource. |

## 11. Implementation sequence and future verification gate

1. Obtain human approval of the specification and this plan before implementing.
2. Add the controller helper and update the two mutation methods; keep the six-field update and API shape intact.
3. Add focused unit tests and the isolated full-context HTTP suite, reviewing datasource overrides and startup runner suppression before execution.
4. After implementation is authorized and isolation confirmed, run from `gestion_be`:

   ```sh
   mvn -Dtest=PlantaControllerTest,PlantaAuthorizationHttpTest test
   mvn test
   mvn verify
   ```

   The `*Test` naming ensures the HTTP suite participates in the current Maven test lifecycle without introducing Failsafe configuration. Inspect any newly added test contexts for database isolation before broader commands.
5. Review the diff against all criteria, confirm no role/tenant changes, no persistent database artifacts and no unrelated files, then obtain the separate implementation code review required by the specification.
6. Report actual commands, results, failures/skips and remaining risks. Backend verification is required; frontend verification is not applicable because no frontend changes are planned.

Historical planning-stage verification: relevant source, configuration and tests were inspected without execution at that stage. Current completion status and evidence are recorded in tasks.md.


## 12. Completion and deferred follow-ups

Feature 001 is complete per the user's verification/approval confirmation. The independent verdict was **APPROVED WITH NON-BLOCKING OBSERVATIONS**. The acceptance criteria remain unchanged. Missing-child DELETE intentionally returns 404; existing role/tenant behavior is preserved.

Implementation deviation: `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` was added beyond the four originally planned implementation files. It selects subclass mocking suite-wide. Record this impact as a non-blocking observation; its original adoption rationale is not available in the retained evidence. Retain it pending a demonstrated need to change it. See [Mockito limitations and reusable checks](../../docs/testing-checklist.md).

**Historical Phase 1 status:** standalone safeguards were not yet implemented at that stage. Phase 2 subsequently added invalid profile defaults and a pre-singleton configuration guard, then completed verification. The JDBC metadata guard remains supplementary. See the Phase 2 status below.

**Concurrency investigation — DEFERRED; no production change authorized.** Ownership checking and mutation are not atomic under concurrent reparenting, deletion or project/company reassignment. Investigate when atomic concurrent ownership becomes a requirement or new ownership-changing writers are introduced. Inventory every relevant writer and compare scoped conditional mutations with coordinated locking/versioning across the child and tenant relationships. Adding `@Transactional` or a scoped lookup alone does not establish the guarantee. Define deterministic race tests against the intended database engine; H2 results do not prove another engine's locking semantics. Preserve this accepted limitation without reopening Feature 001 or silently expanding its criteria.

Future SDD work follows the shared AGENTS.md milestones and checklist rather than repeating the historical per-task gates. Full verification normally uses focused checks followed by `mvn verify`; Feature 001's historical command requirements remain recorded as originally planned.


## Phase 2 implementation status

Implemented under separate user approval; **execution verification complete: 164 tests, zero failures/errors/skips and BUILD SUCCESS**. Independent Phase 2 review was deferred by the user because H2 is provisional pending PostgreSQL migration; this does not constitute independent approval. The profile-scoped main-source static BeanFactoryPostProcessor and pure policy enforce an explicit datasource/JWT contract, strict UUID-v4 H2 memory URL and configuration allowlists. The HTTP suite adds only two dynamic property registrations; its generated values, 107-test baseline coverage, seeder mocks and metadata guard are retained. Profile defaults are invalid, preventing implicit production URL/key inheritance.

New policy tests perform configuration inspection only. New lifecycle tests use minimal contexts with inert datasource/JPA factory sentinels, including profile resource loading and standalone activation without it. A separate minimal Spring test checks dynamic-property ordering. The new suites and existing regression tests have passed; evidence provenance is recorded in tasks.md under Phase 2 verification record. No production behavior outside the test profile, business logic, acceptance criterion, Maven dependency, JWT service or Mockito setting was changed.

Verification sequence completed: pure policy suite (51), lifecycle/property-ordering suites (6), existing regression suites (107), and full `mvn verify` (164). See the Phase 2 verification record in tasks.md for evidence sources. No new execution is authorized by this record. Custom early connection factories and post-validation property mutations are outside the current guard's guarantee; standalone valid configuration does not suppress real seeders. Concurrency investigation remains deferred.
