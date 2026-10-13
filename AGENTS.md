# Backend Agent Instructions

## Stack
Java 17, Spring Boot 3.2, Maven, Spring MVC, Spring Data JPA, Spring Security, JWT and H2.

## Architecture
The codebase uses domain, application and infrastructure packages, with partial ports-and-adapters conventions.

Follow existing patterns where appropriate. Do not assume the domain layer is fully isolated from JPA or infrastructure dependencies.

Prefer existing use cases, repository abstractions and DTO conventions. Avoid introducing new direct controller-to-repository dependencies.

## Security
- Validate authentication, authorization and company/project ownership.
- Pay special attention to nested resources and cross-tenant access.
- Do not assume ADMINISTRADOR has platform-wide permissions without a confirmed business rule.
- Do not expose the H2 console, demo credentials or fallback JWT secrets in publicly accessible environments.
- Do not run demo-reset or destructive operations automatically.

## Persistence
- Do not modify tracked database files.
- Do not introduce destructive schema changes without explicit approval.
- Do not assume schema migrations are versioned; no migration framework was identified during discovery.

## Verification
- Inspect Maven configuration before choosing build and test commands.
- Prefer focused JUnit and Mockito tests for changed behavior.
- Add integration and authorization tests when modifying persistence or access control.
- Report any checks that could not run.


## Verification commands and execution approval

Run commands from `gestion_be`. Inspect `pom.xml` and selected test contexts first; command names alone do not establish isolation.

| Classification | Command | Execution scope |
| --- | --- | --- |
| Focused context-free tests | `mvn -Dtest=PlantaControllerTest,UsuarioDemoSeederTest test` | Current selected classes use mocks, without Spring or a database. |
| Focused HTTP integration tests | `mvn -Dtest=PlantaAuthorizationHttpTest test` | Starts Spring and isolated H2; requires reviewed execution approval. |
| All tests | `mvn test` | Includes HTTP integration tests, not just unit tests. |
| Full backend verification | `mvn verify` | Includes the test phase under the current Maven configuration. |

- Approve context-starting verification once against the reviewed isolation configuration and listed commands, including retries within that scope. Approval includes fixture creation and cleanup exclusively in the verified ephemeral database; persistent data operations require separate explicit approval.
- Renew approval for expanded command scope, newly included unreviewed contexts or changes to isolation assumptions. Reuse existing approval otherwise; general implementation approval alone is insufficient.
- Prefer focused checks during implementation and one final `mvn verify`. Do not routinely repeat a full `mvn test` immediately before `mvn verify` without a change, failure or unresolved concern that justifies it. Honor explicitly required command scopes.
- Never execute against the workspace's persistent H2 database. Follow [the reusable testing checklist](docs/testing-checklist.md).
- Report actual commands, results and skipped checks. Attribute pre-existing reports; do not claim a command succeeded solely because a test report exists.
- **IMPLEMENTED AND VERIFIED:** Phase 2 passed 164 tests with no failures/errors/skips and `BUILD SUCCESS`; full verification evidence was supplied by the user. See [the verification record](specs/001-planta-authorization/tasks.md#phase-2-verification-record). Independent Phase 2 review was deferred by the user while H2 is provisional; this is not independent approval. Existing protections remain in place. Valid standalone activation does not install the HTTP suite's seeder mocks.
