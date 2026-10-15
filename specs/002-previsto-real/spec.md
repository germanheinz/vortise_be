# Previsto vs real — demo Excel

Approved in chat: implement using PREV VS REAL.xlsx, Hoja 1, T6:X6,
T36:X36 and T66:X66. No changes to Feature 001 acceptance criteria.

- The planned activity stores quantity, unit, dates, staffing and planned HH.
  Enter official/helper HH per unit; derive existing planned HH fields on the server.
- Daily ObraTarea links to a rubroId belonging to its project. Existing unlinked
  tasks remain readable, explicitly excluded until linked by editing.
- Production = actual quantity - actual HH / planned HH per unit.
- Productivity index = planned HH per unit / actual HH per unit.
- Resource = (earned planned HH - actual HH) / hours per working day (jornales).
- Monetary variance = earned planned labor cost - actual labor cost (positive savings).
- Remaining projection = variance / actual quantity * max(0, planned quantity - actual quantity).
- Use company OFICIAL/AYUDANTE enabled category hourly costs and weekday hours.
  Missing inputs yield null indicators, never invented costs. Current company rates
  apply to all loads, as in Excel; historical rate snapshots are outside demo scope.
- Retain Excel thresholds and labels, explain units/sign and remaining projection.
- Aggregate inputs by activity, then recalculate; never average ratios or add projections.
- Tasks alone supply actual schedule progress; do not also add RegistroHoras.
- Save/edit/delete refreshes indicators. Reload schedule reads latest tasks.
- Preserve tenant ownership, reject foreign activity IDs, nonfinite/negative numbers,
  invalid dates, and prevent removing or changing units of activities with linked tasks.
- Daily expected quantity and compliance derive from actual HH and planned yield (Excel D10),
  rather than accepting a second manual budget.

## API contract
Existing /api/proyectos/{id} PUT: rubros accept id and optional paired
rendimientoOficial/rendimientoAyudante (HH/unit); planned hours derived server-side.
Existing /api/proyectos/{id}/obra-tareas POST/PUT: add rubroId; require date,
quantity and both HH values for new/edited daily loads (explicit zero allowed).
GET /api/proyectos/{id}/obra-tareas/indicadores: company rates/jornada, unlinked
count and per-activity accumulations plus per-load metrics. No writes during reads.
Existing task response adds rubroId. Old fields stay for API compatibility.

## Verification
Context-free Excel arithmetic fixtures, invalid/zero/missing inputs, activity ownership,
identity preservation and aggregation. TypeScript, existing frontend helper tests,
production build. Spring/DB verification requires separate reviewed execution approval.

## Approved demo data extension
Populate the existing demo company/project with coherent company catalogs, labor costs,
calendar, activity-linked daily production, hour summaries, restrictions, budgets and
income certificates. Synthetic actuals represent a simulation through project closure
(February 2027), explicitly identified in load comments, not observations as of today.
Spread production over activity periods and weekdays, respect crew/day capacity, and
include both favorable and unfavorable productivity scenarios. Repeated startup must
not duplicate generated data or replace manual entries. Only precisely recognized,
untouched legacy demo placeholders may be migrated; unrelated tenants remain untouched.
