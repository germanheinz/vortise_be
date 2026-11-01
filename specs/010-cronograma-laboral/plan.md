# 010 — Plan técnico (propuesta)

Plan aprobado por el dueño el 2026-11-01.

1. Añadir `pedidosDeObraDias` como `Integer` nullable a `Proyecto`; exponerlo en `ProyectoCreateDto`, `ProyectoUpdateDto` y `ProyectoDto`, y conservarlo en `CreateProyectoUseCase`, `UpdateProyectoUseCase` y `ProyectoMapper`. Rechazar valores negativos.
2. Añadir `V15__project_restriction_upload_deadline.sql` con columna nullable `pedidos_de_obra_dias INTEGER` y ALTER local idempotente en `schema.sql`. No ejecutar la migración.
3. En `CreateRestriccionUseCase`, comparar `LocalDate.now()` con `fechaInicio.plusDays(pedidosDeObraDias)` y rechazar solo cuando hoy sea posterior al último día permitido. Mantener el bloqueo en el caso de uso común para cubrir todos los clientes; no cambiar el update de restricciones.
4. Extender pruebas context-free del use case para límite inclusivo, vencimiento, plazo/fecha ausentes y validación de negativo. Agregar prueba HTTP de contrato/seguridad si cabe en el contexto de tests aislado ya existente; no ejecutarla sin autorización.
5. Ordenar `rubros` en `ProyectoMapper.toDetailDto` por código `nRubro` numérico ascendente, con fallback determinista para códigos no numéricos y estabilidad por ID. Verificar que la misma respuesta alimente estructura y cronograma sin ordenar por fecha.
6. No cambiar esquema de `cantidadPersonas`, frentes, HH o fechas: esos campos ya existen.
7. Coordinación frontend: `ObraCreateForm` captura el campo; `CronogramaSection` persiste personas y deriva fecha con calendario laboral. La selección de feriados nacionales/locales se documenta también en el SDD frontend.
8. Fuente de tests permitida tras aprobación; Maven, Flyway, H2 persistente y despliegues requieren autorización independiente.
