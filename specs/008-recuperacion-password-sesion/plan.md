# 008 — Plan técnico backend

## Estado

Implementación completa aprobada explícitamente el 2026-10-28 para BE008/FE019. Verificación aislada autorizada: unitarias, HTTP en H2 efímera con guard, mvn verify tras auditoría de contextos, tsc y build FE; correcciones/reintentos dentro del mismo aislamiento. SMTP configurable aprobado, proveedor y activación remota diferidos. No autoriza publicación, despliegue, migración remota, datos persistentes ni envíos reales en este hito.

## Repositorios y contrato

- Backend: `gestion_be`.
- Contrato consumidor: [SDD frontend](../../../gestion_fe/specs/019-recuperacion-password-sesion/plan.md).
- Endpoints nuevos: `POST /api/auth/password-reset/request` y `POST /api/auth/password-reset/confirm`.

## Decisiones propuestas

1. Mantener login email/contraseña y `BCryptPasswordEncoder`; no añadir refresh tokens ni un framework de autenticación nuevo.
2. Agregar entidad/tabla de token de recuperación con usuario, hash SHA-256 del token, creación, vencimiento y consumo. Generar 32 bytes con `SecureRandom`, codificación URL-safe. No persistir ni loguear el valor original.
3. Emitir token de un solo uso con expiración de 30 minutos; invalidar solicitudes previas del usuario. Consumir y cambiar contraseña dentro de una operación transaccional que impida doble uso concurrente.
4. Responder con mensaje/estado genérico para cuentas existentes e inexistentes. Aplicar cooldown y límites en solicitud; concretar mecanismo con el entorno de hosting, evitando dependencia de memoria local como control único en múltiples instancias.
5. Usar `APP_FRONTEND_URL`/origen explícito por entorno para construir el enlace. El proveedor y los secretos SMTP/API deben configurarse fuera del repositorio. No activar envíos a destinatarios reales desde pruebas.
6. Añadir `sessionVersion` a `UsuarioEmpresa` y a los JWT. El filtro debe buscar la cuenta por `userId`, rechazarla si está inactiva o la versión no coincide, y derivar authorities, empresa y `superAdmin` desde el estado actual. Incrementar versión al resetear contraseña, cambiar rol o desactivar/reactivar una cuenta. Esto agrega una lectura de usuario por request autenticado, a cambio de revocación inmediata sin tablas de refresh sessions.
7. Configurar el JWT de acceso con vencimiento máximo de una hora. No emitir refresh token en esta primera fase.
8. Configurar explícitamente un `AuthenticationEntryPoint` REST que responda `401` para ausencia/token inválido/expirado. `AccessDeniedHandler` continúa respondiendo `403`. Actualizar el test HTTP existente que hoy caracteriza `403` para tokens no autenticados.
9. No incluir token, email completo, contraseña ni cuerpo sensible en logs. Mantener respuestas genéricas y sin diferencias de enumeración.
10. Para correo, usar Spring Mail/SMTP estándar con TLS y proveedor transaccional gestionado; el proveedor concreto queda abierto. Confirmar SPF/DKIM/DMARC y remitente autorizado antes de habilitar staging.

## Persistencia y compatibilidad

- Agregar una migración forward-only para token de reset y versión de sesión, sin editar migraciones aplicadas, borrar datos ni usar la H2 persistente para pruebas.
- Revisar compatibilidad de la inicialización H2 local y el esquema/migraciones PostgreSQL de staging antes de decidir los archivos exactos.
- Los usuarios existentes empiezan con versión de sesión cero; los JWT previos sin claim de versión se rechazan y requieren nuevo login tras el despliegue.

## Pruebas propuestas

- Unitarias: token aleatorio/hash, cuenta no encontrada con respuesta genérica, envío solicitado, contraseña BCrypt, expiración, reutilización, reemplazo de token y fallos de correo.
- HTTP con base efímera revisada: `401` ausente/malformado/expirado/versión antigua; `403` rol insuficiente; rol actual, cuenta desactivada, cambio de contraseña y ausencia de acceso después del reset.
- Verificar migración/reinicio en PostgreSQL desechable y pruebas context-free antes de suites que inician Spring, según `gestion_be/AGENTS.md` y `docs/testing-checklist.md`.
- No ejecutar comandos hasta aprobación del comando y del entorno de pruebas.

## Bloqueos / decisiones

- Elegir proveedor de email transaccional y cuenta/remitente de staging.
- Definir límites de envío por cuenta/IP y el punto de aplicación (hosting/WAF o backend persistente).
- Confirmar aceptación de una consulta de usuario en cada request autenticado para revocación/roles inmediatos.

## Concreción local del plan aprobado

Spring Mail/SMTP TLS es la única dependencia adicional, necesaria para el envío aprobado; deshabilitado por defecto, con timeouts. Tokens de32bytes, SHA256,30min, fila única por usuario y bloqueo del usuario al emitir/consumir; hash y vencimiento, nunca valor original persistido. Versionado optimista de usuario evita sobrescritura concurrente de contraseña/versión por login o edición. Cuotas persistentes serializadas por fila guard:60 solicitudes/minuto global,5/IP por15min y1/email por minuto, claves SHA256 y limpieza de ventanas antiguas; IP de conexión, sin confiar en X-Forwarded-For del cliente (WAF/proxy específico se configura al activar proveedor). Respuesta202 uniforme incluso con cuota o correo deshabilitado/fallido. Envíos fuera de transacciones de contraseña; fallo de notificación no revierte la contraseña ya cambiada y se registra solo mensaje genérico. Migración V8 nueva; migraciones V1–V7 intactas. SMTP debe activarse y verificarse antes de declarar entrega remota lista.

El adaptador SMTP utiliza un executor acotado (2 hilos, cola100) para no diferenciar cuentas por latencia de SMTP; nunca persiste cuerpos/tokens en una cola. La pérdida de un proceso puede perder un envío pendiente: se solicita otro enlace. No se incorpora un sistema de colas durable. Fallos solo con mensajes genéricos. Los backups H2 excluyen tokens/cuotas de recuperación y la restauración invalida enlaces y renueva generaciones de sesión; no se restauran sesiones previas junto con datos antiguos. Se adaptan fixtures HTTP a identidades persistidas reales, sin mocks del filtro de autorización. Los tests heredados de ausencia de autenticación cambian explícitamente403→401 según la especificación.

## Correcciones de revisión independiente

Generación inicial/restaurada de sessionVersion impredecible positiva (SecureRandom62bits con margen), no contador0 o max+1 al crear/restaurar: evita reactivar JWT tras restauraciones sucesivas, desaparición de usuario y reutilización de ID. Los incrementos de versión siguen aplicando a cambios de contraseña/rol/actividad. Usuarios existentes migrados conservan0; la firma JWT y el estado vigente siguen siendo obligatorios. FE contrasta Authorization de la request que recibe401 con el bearer actual antes de logout, preservando un login más reciente. SMTP comprueba identidad TLS del servidor explícitamente.
