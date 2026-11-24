# Tasks 016 — Calendario laboral por obra

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)
Estado global: **Fase 1 implementada y verificada con pruebas sin contexto; migración V28 sin ejecutar. Fase 2 pendiente.**

| ID | Fase | Tarea | Depende de | Estado | Criterio |
|---|---|---|---|---|---|
| T-001 | H1 | Descubrimiento, spec y plan | — | Hecho | spec.md y plan.md |
| T-002 | H2 | Aprobación de spec, plan y decisiones D-1 a D-3 | T-001 | Hecho (Fase 1; D-1 y D-2 aprobadas) | Decisiones registradas |
| T-010 | F1 | Migración V28 y campos en `Proyecto` | T-002 | Escrita; **sin ejecutar, a revisar** | Valores derivados de la empresa |
| T-011 | F1 | Crear/editar obra con los 4 campos (API) | T-010 | Hecho (`CalendarioLaboralObraTest`, 3 pruebas) | Pruebas de caso de uso |
| T-012 | F1 | `CalendarioObraFields` en crear/editar obra | T-011 | Hecho; sin prueba de navegador | Valores iniciales y guardado |
| T-013 | F1 | Calendario efectivo en el cronograma (sin selector: se edita al crear/editar la obra) | T-011 | Hecho (prueba Node de `calendarioEfectivo`) | Fin calculado con el calendario de la obra |
| T-020 | F2 | Feriados en Java y factor de día | T-002 | Pendiente | Pruebas gemelas con el frontend |
| T-021 | F2 | Costo real y previsto con horas ponderadas (BE) | T-020, T-011 | Pendiente | HH e indicadores sin cambio |
| T-022 | F2 | Costos con recargo en el frontend | T-021, T-013 | Pendiente | Pruebas Node |
| T-030 | V | Verificación completa por repositorio | T-012, T-013, T-022 | Parcial (ver evidencia) | Resultados reales |
| T-031 | V | Revisión independiente y cierre | T-030 | Pendiente | Veredicto y riesgos |

## Evidencia Fase 1
- `gestion_be`: `mvn -o -Dtest=CalendarioLaboralObraTest,CreateProyectoSubcontratistaTest,Quality14RulesTest,ProyectoMapperTest,RubroPlanningTest,RubroEstructuraUseCasesTest test` → 54 pruebas, 0 fallos (sin Spring ni base de datos). No se ejecutaron `mvn verify`, las pruebas HTTP ni la migración V28 contra Flyway/PostgreSQL.
- `gestion_fe`: `node --test tests/*.test.cjs` → 289 pruebas, 0 fallos; `tsc --noEmit --incremental false` sin errores; `npm run build` correcto (el primer intento lo corta un aviso de seguridad de Next.js; se repite y pasa).
- Desviación del spec: horas de sábado = 0 con "trabaja sábados" muestra advertencia en lugar de bloquear.

## Aprobaciones de ejecución a solicitar (agrupadas)
- Revisar el SQL de V28 y su ejecución (Flyway sobre PostgreSQL de prueba) por separado.
- Pruebas HTTP/de contexto del backend (`mvn verify`) sobre la base aislada.
- Fase 2: edición de los archivos del plan, sin commit ni push.
