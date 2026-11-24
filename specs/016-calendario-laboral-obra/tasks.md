# Tasks 016 — Calendario laboral por obra

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)
Estado global: **pendiente de aprobación**. Nada implementado.

| ID | Fase | Tarea | Depende de | Estado | Criterio |
|---|---|---|---|---|---|
| T-001 | H1 | Descubrimiento, spec y plan | — | Hecho | spec.md y plan.md |
| T-002 | H2 | Aprobación de spec, plan y decisiones D-1 a D-3 | T-001 | Pendiente | Decisiones registradas |
| T-010 | F1 | Migración V28 y campos en `Proyecto` | T-002 | Pendiente | Valores derivados de la empresa |
| T-011 | F1 | Crear/editar obra con los 4 campos (API) | T-010 | Pendiente | Pruebas de caso de uso y 403 |
| T-012 | F1 | `CalendarioObraFields` en crear/editar obra | T-011 | Pendiente | Valores iniciales y guardado |
| T-013 | F1 | Selector en cronograma y calendario efectivo | T-011 | Pendiente | Fin recalculado |
| T-020 | F2 | Feriados en Java y factor de día | T-002 | Pendiente | Pruebas gemelas con el frontend |
| T-021 | F2 | Costo real y previsto con horas ponderadas (BE) | T-020, T-011 | Pendiente | HH e indicadores sin cambio |
| T-022 | F2 | Costos con recargo en el frontend | T-021, T-013 | Pendiente | Pruebas Node |
| T-030 | V | Verificación completa por repositorio | T-012, T-013, T-022 | Pendiente | Resultados reales |
| T-031 | V | Revisión independiente y cierre | T-030 | Pendiente | Veredicto y riesgos |

## Aprobaciones de ejecución a solicitar (agrupadas)
- Editar los archivos listados en el plan (ambos repos), sin commit ni push.
- Ejecutar pruebas focales y la verificación de cada repo; sin levantar el backend contra datos reales.
- Migración V28: revisión del SQL y su ejecución, por separado.
