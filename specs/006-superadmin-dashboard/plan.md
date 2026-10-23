# 006 — Plan técnico de backend

## Estado

APROBADO — 2026-10-23. Ejecutar dentro del alcance del spec; no ejecutar pruebas ni build sin autorización separada.

## Cambios propuestos

1. Crear DTOs de resumen/lista dedicados a superadmin. Evitar devolver entidades JPA y restringir campos a los necesarios.
2. Ampliar repositorios JPA con consultas paginadas/filtradas para empresas, usuarios, suscripciones y pagos. Añadir proyecciones/agregaciones para KPIs en vez de cargar tablas completas.
3. Crear casos de uso de consulta global y un controlador `/api/admin/...` con `@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")` para resumen, empresas, usuarios, suscripciones y pagos.
4. Reutilizar el estado local sincronizado por `StripeWebhookUseCase`; no llamar Stripe por cada fila ni reportar datos de webhook como tiempo real. No cambiar el contrato de Checkout ni webhooks salvo que pruebas indiquen que los estados actuales no bastan.
5. Calcular alertas de pago fallido, `past_due` y prueba próxima a vencer con límites temporales documentados y zona horaria explícita. Evitar contar la misma factura varias veces.
6. Añadir pruebas context-free para agregaciones/filtros y pruebas HTTP aisladas de autorización y paginación según el patrón de `PlantaAuthorizationHttpTest`. No ejecutar sobre H2 persistente.
7. Mantener esquema aditivo o sin cambios si los datos disponibles bastan. No modificar datos, seeds ni backup.

## Contratos de API propuestos (pendientes de detalle y aprobación)

- `GET /api/admin/dashboard`: KPIs y alertas con fecha de cálculo.
- `GET /api/admin/empresas?page=&size=&q=`: empresa y métricas de usuarios/suscripción.
- `GET /api/admin/usuarios?page=&size=&q=&empresaId=&activo=`: perfil mínimo de cuenta.
- `GET /api/admin/suscripciones?page=&size=&estado=&empresaId=`: titular, plan, estado y fechas.
- `GET /api/admin/pagos?page=&size=&estado=&desde=&hasta=`: empresa/titular, monto en centavos, moneda, estado y fecha.

Todos retornarían paginación explícita (elementos, página, tamaño, total) y errores coherentes con el contrato existente. Los nombres y formas finales se fijarán antes de implementar el frontend.

## Riesgos / controles

- Los agregados no deben contar la cuenta interna superadmin como cliente ni sumar montos de monedas distintas.
- Asociaciones JPA lazy deben resolverse con proyecciones/consultas controladas para evitar N+1.
- Tests de autorización deben comprobar un token superadmin y un token administrador/usuario común.
- No agregar migraciones destructivas ni tocar H2 persistente.

## Contrato final implementado

Todos los endpoints son `GET` y requieren exactamente `ROLE_SUPER_ADMIN`:

- `GET /api/admin/dashboard`: `calculadoEn`, `datosActualizadosEn`, `empresas`, `usuariosActivos`, `usuariosInactivos`, `suscripcionesPorEstado[{estado,cantidad}]`, `pagosRecientesPorEstadoYMoneda[{estado,moneda,cantidad,montoCentavos}]`, alertas `pagosFallidos`, `suscripcionesPastDue`, `suscripcionesInactivas`, `pruebasPorVencer`, `ventanaPruebasDias` y `fuentePagos`. Pagos recientes cubren 30 días; pruebas por vencer cubren los próximos 7 días.
- `GET /api/admin/empresas?page=0&size=20&q=`: página `content[{id,nombre,creadoEn,usuariosActivos,planCodigo,estadoSuscripcion}]`.
- `GET /api/admin/usuarios?page=0&size=20&q=&empresaId=&activo=`: página `content[{id,empresaId,empresaNombre,nombre,email,rol,activo}]`.
- `GET /api/admin/suscripciones?page=0&size=20&estado=&empresaId=`: página `content[{id,titularTipo,titularNombre,titularEmail,empresaNombre,planCodigo,planNombre,estado,finPrueba,finPeriodoActual,cancelaAlFinal,actualizadoEn}]`.
- `GET /api/admin/pagos?page=0&size=20&estado=&desde=YYYY-MM-DD&hasta=YYYY-MM-DD`: página `content[{id,empresaNombre,titularNombre,titularEmail,planCodigo,estado,montoCentavos,moneda,fecha}]`. `hasta` es inclusivo por día y las fechas se interpretan en UTC.
- Todas las listas usan `page`, `size`, `totalElements` y `totalPages`; páginas 0-based y tamaño máximo 100. Los resúmenes de pagos agrupan y ordenan por estado/moneda, nunca suman monedas distintas.
- Los pagos reflejan exclusivamente `PAGADO`/`FALLIDO` guardados por webhooks; no se consulta Stripe en estas lecturas. Se excluye la empresa plataforma de empresas y el usuario superadmin de usuarios, conteos, suscripciones y pagos.
