# Plan 016 — Calendario laboral por obra

Spec: [spec.md](./spec.md) · Estado: **pendiente de aprobación**.

## Fase 1 — Configuración y cronograma (sin costos)
**Backend**
1. Migración Flyway `V28__calendario_laboral_obra.sql`: agrega 4 columnas booleanas `NOT NULL` a la tabla de obras con valor inicial copiado del calendario de su empresa (obras sin calendario: sábado sí, domingo y dobles no). Aditiva y reversible borrando columnas. **Requiere aprobación de ejecución** (la base puede ser persistente; no se ejecuta contra datos reales sin aprobación).
2. `Proyecto` + builder: campos y getters/setters.
3. DTOs/casos de uso de crear y editar obra: aceptar y devolver los campos; si faltan al crear, derivarlos de la empresa. Validación y autorización iguales a editar obra.
4. Pruebas (`mvn test` focal): crear con y sin campos, editar, derivación desde la empresa, 403 sin permiso.

**Frontend**
5. `types/index.ts` y `lib/api/obras.ts`: campos nuevos.
6. Componente compartido `CalendarioObraFields` (4 checks, textos en español) usado por `ObraCreateForm` (crear y editar) y por el cronograma.
7. `app/proyectos/[id]/cronograma/page.tsx` y `CronogramaSection`: selector arriba, guardado en la obra, calendario efectivo = empresa + overrides de obra; `hoursForWorkday` recibe `trabajaSabado`/`trabajaDomingo`. Validación de horas de sábado = 0.
8. Pruebas Node en `tests/cronograma.test.cjs` y `tests/cronograma-hours-groups.test.cjs`.

## Fase 2 — Costos con recargo
**Backend**
9. `FeriadosArgentina` (Java) con las mismas reglas que `argentinaNationalHolidays` y pruebas con las mismas fechas.
10. `FactorDiaLaboral`: función pura (fecha, calendario de obra) → factor; y reparto de horas de una carga en sus días.
11. `GetPrevistoRealUseCase`/`PrevistoRealCalculator`: horas ponderadas reales; el costo y el desvío usan horas ponderadas; HH e indicadores sin cambio.
12. `GetEmpresaDashboardObrasUseCase`: costo previsto con horas ponderadas del cronograma (portar el reparto diario del cronograma).
13. Pruebas unitarias del factor y del reparto, y de regresión de `PrevistoRealCalculator`.

**Frontend**
14. `lib/cronograma-laboral.ts`: devolver horas ponderadas previstas por tarifa (OF/AY) además de `fin`/`dias`.
15. `CronogramaSection`, `reportes-obra`, `PrevistoRealPanel`: costos con horas ponderadas; textos que expliquen el recargo.
16. Pruebas Node de costo con sábado/domingo/feriado.

## Verificación (por repositorio)
- `gestion_be`: según [AGENTS.md](../../AGENTS.md); primero pruebas focales y luego la suite completa. Sin levantar el contexto con datos reales sin aprobación.
- `gestion_fe`: `node --test tests/*.test.cjs`, `./node_modules/.bin/tsc --noEmit --incremental false`, `npm run build`.
- Verificación de compatibilidad de contrato BE↔FE y revisión independiente antes del cierre.

## Riesgos
- Duplicar la lógica de feriados y reparto en Java y TypeScript: mitigado con pruebas gemelas sobre las mismas fechas.
- Cambio visible en costos de obras que ya trabajan sábados: informar en el cierre.
- Migración sobre datos existentes: revisar el SQL antes de ejecutarlo.
