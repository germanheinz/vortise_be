# Implementation plan (approved scope)
1. Reuse company labor categories/calendar and existing planned-HH persistence.
2. Add stable activity linkage; preserve rubro identity through project edits.
3. Implement one backend comparison calculator and read-only aggregation use case.
4. Wire daily tasks and schedule to same task source; add comparison UI and yield inputs.
5. Run pure tests and frontend checks; statically review persistence/security changes.

Persistence: one nullable rubro_id foreign key on obra_tareas; no backfill by ambiguous names,
no database reset, no production startup by the agent. Existing rubros retain IDs.

## Demo seeder extension
Reuse DataSeeder and existing entity/repository contracts. Isolate deterministic fixture
generation in DemoOperationalSeed, requiring the demo company fiscal identifier and
project identity. Persist new activity IDs before linking daily tasks. Derive daily
expected production through the same comparison calculator; hour summaries mirror loads
and never contribute a second time to schedule actuals. Fill missing catalog entries
without replacing configured rates. Financial fixtures use company labor costs, with
no assumed USD conversion. Verify with plain objects and mocked repositories only;
actual JPA startup and persistent seeder execution remain unverified and unexecuted.
