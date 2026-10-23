# 006 — Dashboard de operaciones Superadmin

## Estado

APROBADO — 2026-10-23. Implementación autorizada en ambos repositorios. Tests/build y cualquier consulta o cambio de datos quedan sujetos a autorización separada.

## Contexto confirmado

- El superadmin es una cuenta marcada con `superAdmin=true` y autoridad `ROLE_SUPER_ADMIN`; no es un valor del enum de roles.
- `GET /api/empresas` ya devuelve todas las empresas al superadmin; el resto de listados globales no tiene API dedicada.
- Los pagos de suscripciones tienen modelo local. `StripeWebhookUseCase` procesa `invoice.paid` y `invoice.payment_failed` y registra su estado en `PagoSuscripcion`.
- No existe un registro de errores técnicos de aplicación ni una integración de monitoreo (Sentry/OTel/etc.). No hay canal de reporte de errores del cliente.
- El usuario confirmó que él será el único superadmin, la primera versión será de consulta y alertas, y el reporte/chat de errores por cliente se posterga.

## Objetivo

Proporcionar al superadmin una vista de solo lectura para conocer el estado global de clientes, usuarios, suscripciones y pagos; identificar situaciones que requieren atención y navegar a su detalle. La cuenta superadmin no se incluye en métricas de clientes/usuarios de plataforma.

## Alcance propuesto para MVP

1. **Resumen global**: cantidad de empresas, usuarios activos/inactivos, suscripciones por estado y pagos recientes por estado. Los importes se agrupan por moneda y estado; no se presentan como ingresos recurrentes ni se convierten monedas.
2. **Alertas derivadas de datos locales**: pagos fallidos recibidos por webhook, suscripciones `past_due`/inactivas y pruebas próximas a vencer. Cada tarjeta indica la fuente y fecha de actualización cuando corresponda. No se prometen notificaciones en tiempo real.
3. **Empresas**: listado global paginado/buscable con alta, cantidad de usuarios activos y resumen de plan/estado de suscripción.
4. **Usuarios**: listado global paginado y filtrable por empresa/estado, mostrando nombre, correo, rol y estado activo. No se inventa “último acceso”: no existe actualmente ese dato.
5. **Suscripciones y pagos**: listas globales de solo lectura con empresa/titular, plan, estado, fechas relevantes y detalle de pagos, incluyendo pagos fallidos conocidos localmente.
6. **Errores/incidentes**: el reporte conversacional/formulario de errores por cliente queda fuera de esta versión. La aplicación tampoco tiene actualmente una fuente fiable de incidentes técnicos para listar. La captura y gestión de errores técnicos quedan fuera del MVP hasta elegir e integrar una fuente de observabilidad; no se mostrarán incidentes ficticios ni una lista vacía que sugiera cobertura inexistente.
7. **Acciones prohibidas en MVP**: editar/suspender usuarios o empresas, cambiar planes/suscripciones, actuar sobre pagos, restaurar datos desde el dashboard y mutar recursos de clientes. La pantalla de respaldo existente permanece separada.

## Seguridad y privacidad

- Todos los endpoints nuevos de administración requieren exactamente `ROLE_SUPER_ADMIN`, además de las reglas generales de autenticación.
- ADMINISTRADOR de empresa y otros roles no pueden acceder a métricas/listas globales.
- Aplicar paginación y filtros en servidor; no descargar todos los usuarios/pagos al navegador.
- Exponer solo los campos necesarios. Nunca exponer hashes de contraseña, tokens, IDs de cliente secretos de Stripe ni payloads sensibles de webhook.
- No registrar PII innecesaria en logs.

## Fuera de alcance / decisiones pendientes

- Chat o formulario para errores reportados por clientes.
- Sentry u otro proveedor, captura de stack traces, correlación de trazas y seguimiento de incidentes técnicos.
- MRR/ARR, churn, contabilidad, conciliación con Stripe y conversiones de moneda.
- Último acceso de usuario, pues el modelo no lo registra.
- Cambios de estado, acciones sobre suscripciones, pagos o configuración comercial.
- Reemplazo de H2, despliegue, migraciones destructivas o configuración de producción.

## Criterios de aceptación propuestos

1. El superadmin ve un resumen global consistente con las listas fuente; se excluyen la empresa interna de plataforma y sus cuentas (incluida la cuenta superadmin), sin mezclar monedas.
2. Listas de empresas, usuarios, suscripciones y pagos permiten búsqueda/filtros y paginación desde backend.
3. Los webhooks `invoice.paid` y `invoice.payment_failed` se representan con su estado local sin duplicar facturas.
4. Alertas se derivan de estados/fechas persistidos y muestran estados vacíos/error explícitos; un fallo de API no se presenta como cero.
5. Todos los endpoints están cubiertos por pruebas de autorización para superadmin permitido y usuario no-superadmin denegado.
6. No existe acción de modificación de datos desde el nuevo panel; respaldo sigue en su ruta y flujo actuales.
7. No se presenta una bandeja de bugs como si existiera integración técnica; la observabilidad se documenta como siguiente etapa.
