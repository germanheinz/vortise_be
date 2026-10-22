# 005 — Tareas y estado

- [x] Registrar alcance y políticas aprobadas en SDD propio.
- [x] Actualizar modelo y seeder BASIC/PRO; eliminar interacciones Stripe.
- [x] Actualizar pruebas unitarias del seeder y política de DB inicializada.
- [x] Corregir fixture/assertions del caso de uso para reflejar USD 9/19 y cubrir IDs Stripe configurados después del primer seed.
- [x] Revisar diagnósticos editor de catálogo y pruebas: sin errores en modelo, seeder y prueba del seeder; en `SuscripcionUseCaseTest` queda un aviso de null-safety preexistente en helper genérico `withId`, ajeno al precio probado.
- [x] Revisión independiente estática: corregido el fixture de precios anterior (USD 1/2) y añadido assert API de USD 9/19; no se hallaron otros defectos accionables.
- [x] Cerrar con archivos, verificaciones ejecutadas y limitaciones.

## Restricciones de ejecución

No ejecutar comandos, tests, Maven/build, API, DB ni llamadas/escrituras remotas a Stripe. Mantener cambios locales. Las pruebas se reportarán como no ejecutadas.

## Evidencia y cierre

- Diagnósticos editor read-only: sin errores en `PlanSuscripcion`, `PlanSuscripcionSeeder`, `PlanSuscripcionSeederTest` y `SeedersSkipOnInitializedDatabaseTest`.
- `SuscripcionUseCaseTest` muestra un diagnóstico de null-safety preexistente en el helper `withId` (línea 173); no corresponde al cambio de precio.
- Inspección de referencias: el seeder ya no depende de `StripeGateway`; el método de creación automática Stripe permanece en su puerto/adaptador, pero este flujo no lo invoca.
- Maven/tests/build/API/DB y Stripe remoto: omitidos explícitamente por instrucción.
- Límite aceptado: se requiere que el operador cree los recurring Prices y configure las variables; no se valida remotamente el importe del Price ID.
