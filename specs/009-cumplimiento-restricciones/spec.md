# 009 — Área y cumplimiento de restricciones

## Aprobación y alcance

Enfoque y fórmula aprobados explícitamente por el dueño el 2026-10-29. Implementación coordinada con [frontend](../../../gestion_fe/specs/020-cumplimiento-restricciones/spec.md). Se autorizaron código, fuente de pruebas y documentación; no ejecución de pruebas/contextos, migraciones, acceso a bases persistentes, despliegues o escrituras remotas. Seguir [AGENTS.md](../../AGENTS.md).

## Comportamiento

- Agregar Área independiente de Responsable; no inferirla desde la persona, el rol o el nombre del rubro. El frontend ofrece nombres del catálogo de empresa AREA_RESTRICCION.
- Persistir el nombre como snapshot opcional de hasta 120 caracteres en la restricción. Los registros existentes conservan área nula y aparecen como Sin área; no hay backfill inventado ni sincronización retroactiva al renombrar un catálogo.
- Guardar fecha de solicitud, máxima y real de entrega. Rechazar máxima o real anteriores a solicitud cuando ambas fechas existan. Permitir fechas ausentes para datos históricos y restricciones pendientes.
- Conservar prioridad, estado, orden, comentarios, rubro y autorización por empresa/obra actuales. Un cambio de estado no crea una fecha real artificial.

## Contrato API

- POST /api/proyectos/{id}/restricciones: campo aditivo `area?: string | null`; los otros campos conservan contrato. GET de detalle/listados devuelve `area: string | null`.
- PATCH /api/restricciones/{id}: mantener prioridad/estado/orden; añadir area, responsable y las tres fechas. Area/responsable omitidos o null preservan el valor; cadena vacía lo limpia.
- Para fechas, los campos no nulos se actualizan de forma parcial. `actualizarFechas: true` reemplaza explícitamente las tres fechas y permite limpiar alguna con null. Omitir el flag y las fechas preserva el comportamiento del tablero.
- La actualización valida antes de mutar y guarda/mapea en una transacción. No cambiar endpoints, roles ni validaciones de pertenencia existentes.

## Métrica aprobada

El cálculo y su presentación están especificados en el SDD frontend. Días corridos; entrega real igual a máxima cumple. Porcentaje = entregadas a tiempo / (entregadas evaluables + pendientes vencidas) * 100. Pendientes en plazo y fechas incompletas no entran en el denominador. Separar empresas incluso cuando comparten nombre de área.

## Aceptación backend

1. Crear/leer/actualizar preserva área normalizada y fechas; área vacía se representa como null.
2. No permitir fechas invertidas ni áreas mayores de 120 caracteres.
3. PATCH anterior de estado/orden no borra área, responsable ni fechas; no fabricar entrega al marcar resuelta.
4. Acceso ajeno o anónimo sigue denegado en la ruta real; las nuevas propiedades no amplían permisos.
5. Migración aditiva V12 y esquema local compatibles, sin cambios a migraciones anteriores ni destrucción de datos.