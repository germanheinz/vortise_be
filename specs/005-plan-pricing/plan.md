# 005 — Plan técnico

Estado: aprobado. Especificación: [spec.md](spec.md).

## Decisiones de implementación

- Reutilizar `PlanSuscripcionRepository.findByCodigo` y `save`; mantener `SeedRunPolicy` como autoridad para la creación inicial, pero no usarla para omitir la migración limitada BASIC/PRO.
- Agregar setters de importe y moneda al modelo. No actualizar beneficios ni otras propiedades de filas existentes.
- Tratar 100 centavos para BASIC y 200 para PRO como únicos valores legacy migrables. La migración escribe 900/1900 y `usd`. Solo modifica filas con esos precios exactos.
- Aplicar `STRIPE_PRICE_BASIC`/`STRIPE_PRICE_PRO` a planes creados y a los legacy migrados; sin variable, dejar `null` al migrar. No sobreescribir IDs ni precios de planes manuales con importe distinto del legacy.
- Eliminar la dependencia del `StripeGateway` del seeder, incluido cualquier enlace automático de precios. Registrar nombres de variables requeridas cuando el checkout de una fila migrada queda deshabilitado; nunca registrar los valores.
- No inyectar ni modificar `SuscripcionRepository`: el arranque solo persiste entidades `PlanSuscripcion`.

## Cambios

1. Implementar sincronización de catálogo para fresh y bases existentes.
2. Ajustar pruebas unitarias del seeder y expectativa de `SeedersSkipOnInitializedDatabaseTest`.
3. Añadir SDD con alcance, límites y evidencia pendiente.

## Verificación

Por instrucción del usuario, no ejecutar Maven, tests, build, API, DB ni comandos. Revisar únicamente diagnósticos editor read-only tras los cambios. La ejecución de pruebas queda a cargo del usuario.
