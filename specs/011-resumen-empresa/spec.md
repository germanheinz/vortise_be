# 011 — Resumen y restricciones de empresa en una sola consulta

Estado: propuesta pendiente de aprobación. Sin cambios de código. Origen: auditoría de uso del backend en `gestion_fe/specs/023-consultas-por-contexto`.

## Problema

El frontend arma el resumen de una empresa con 1 + 3N peticiones (N obras: restricciones, tareas y horas de cada una) y sus restricciones con 1 + N. El contador del menú repite ese patrón. Además el indicador «Cargas cumplidas» es siempre `0/0`: el frontend lo calcula con `rubros` de cada obra, pero el listado de obras usa `ProyectoMapper.toSummaryDto`, que devuelve `rubros` nulo.

## Contrato propuesto

### `GET /api/empresas/{id}/resumen`

```json
{ "empresaId": 1, "obras": 4, "restricciones": 37, "horasRegistradas": 812.5, "cargasVinculadas": 120, "cargasCumplidas": 85 }
```

- `obras`: obras de la empresa.
- `restricciones`: restricciones de todas sus obras.
- `horasRegistradas`: suma de `RegistroHoras.horas` de sus obras; `null` en horas cuenta como 0.
- `cargasVinculadas`: tareas de obra con actividad (`rubroId`) asignada.
- `cargasCumplidas`: tareas vinculadas cuyo `porcentajeCumplimiento` es ≥ 100, calculado con **la misma regla que ve la UI** (`CreateObraTareaUseCase.toDto`). La columna persistida no sirve: cuando la tarea tiene actividad, el DTO la ignora y recalcula desde horas y cantidad real. Valor nulo cuenta como no cumplida.
- Empresa sin obras: todos los valores en 0.

### `GET /api/empresas/{id}/restricciones`

`List<RestriccionDto>` con las restricciones de todas las obras de la empresa, mismo DTO que `GET /api/proyectos/{id}/restricciones`. Una consulta en lugar de 1 + N.

### Autorización y errores (ambos)

- `identity.requireEmpresa(id)` antes de cualquier lectura, igual que `GET /api/empresas/{id}/obras/pagina`; sin acceso: 403 sin revelar existencia.
- Empresa inexistente (con acceso): 404 «Empresa no encontrada».
- Solo lectura. Sin cambios de esquema, datos ni endpoints existentes.

## Aceptación

1. Los valores coinciden con la suma de los endpoints por obra para la misma empresa.
2. `cargasCumplidas` coincide con contar en `GET /api/proyectos/{id}/obra-tareas` las tareas con `rubroId` y `porcentajeCumplimiento ≥ 100`.
3. Una empresa ajena devuelve 403 y no ejecuta consultas de datos; colaborador, administrador y superadmin siguen la política vigente de `requireEmpresa`.
4. Número de consultas SQL acotado y no proporcional a las peticiones de red del frontend; el coste interno crece con las obras y se registra como límite.
5. Sin regresiones en los endpoints actuales.

## Fuera de alcance

Cache HTTP/ETag, caché de servidor, corregir `GET /api/restricciones` (`findAll()` filtrado en memoria), paginación de resumen, y cualquier cambio de esquema.

## Impacto en el frontend (repositorio aparte, tras aprobar el contrato)

Tipo `EmpresaResumenDto`; el dashboard y los contadores del menú usan una sola petición (el botón «Cargar resumen» deja de ser necesario); restricciones de empresa y «Cargar otras obras» usan el listado único; se eliminan los helpers de fan-out si quedan sin uso.
