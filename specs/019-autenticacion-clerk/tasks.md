# 019 — Estado y evidencia backend

## Estado

- [x] Discovery de autenticación actual, sesión por cookies, invitaciones, registro, super admin y código Clerk previo.
- [x] Decisiones de producto confirmadas por el usuario (2026-11-26): intercambio de sesión, solo Clerk, registro con Clerk, Google, super admin vinculado.
- [x] Spec y plan aprobados por el usuario (2026-11-26). Alcance: implementación BE019/FE041 y tests unitarios sin contexto.
- [ ] Tests HTTP y `mvn verify`: **no autorizados** (2026-11-26); pendientes de aprobación.
- [x] Implementación (2026-11-26).
- [x] Tests unitarios sin contexto: 51/51 (ver evidencia).
- [ ] Tests HTTP y `mvn verify` (no autorizados).
- [x] Revisión independiente de seguridad (2026-11-26): sin hallazgos de severidad alta.
- [ ] Cierre (pendiente de tests HTTP, `mvn verify` y prueba manual).

## Revisión independiente

| Hallazgo | Severidad | Estado |
|---|---|---|
| Refresco de JWKS sin límite si la API de Clerk falla; requests esperando el lock | Media | Corregido: un único intento por minuto (`AtomicReference`), sin bloqueo; test agregado |
| `azp` opcional | Baja | Corregido: `azp` obligatorio; test agregado |
| Super admin ligado a la seguridad del buzón/Google de `APP_SUPERADMIN_EMAIL` | Baja (diseño) | Aceptado; recomendación: exigir MFA en Clerk para esa cuenta o fijar su `clerkUserId` |
| Intención de Google sin vencimiento y destino sin revalidar | Baja | Corregido en FE041: vence a los 10 minutos y `safeReturnPath` |
| Configuración faltante bloquea todos los accesos sin aviso | Riesgo de despliegue | Mitigado: error explícito en el log al arrancar. No se hace fallar el arranque para no romper los tests Postgres con perfiles `production`/`staging`, que no pudieron ejecutarse |

Riesgos residuales aceptados: un token de Clerk (≈60 s de vida) puede reutilizarse dentro de su vigencia; cerrar sesión en Clerk desde otra pestaña no cierra la sesión del backend; el alta directa de usuarios por el administrador todavía pide contraseña, que no se usa con el login por contraseña apagado.

## Tareas

1. Puerto `IdentidadExternaVerificador` + `IdentidadVerificada`; `ClerkTokenVerifier` como adaptador.
2. `IdentidadExternaService` y `CuentaNoRegistradaException`; eliminar `ClerkUserSyncService`.
3. `UsuarioEmpresa.conIdentidadExterna(...)`.
4. `ClerkAuthController`: `/auth/clerk/sesion`, `/auth/clerk/registro`.
5. Aceptación con identidad en invitaciones de usuario y de equipo.
6. `PasswordLoginGuard` + flag + `410`; perfiles de test con flag activo.
7. Eliminar `ClerkJwtInterceptor`, `ClerkTokenValidator`, `InvitacionController` y DTO asociados.
8. `SuperAdminSeeder` sin contraseña utilizable.
9. Tests unitarios y HTTP.

## Evidencia de implementación

Comando (sin Spring ni base de datos):

`mvn -Dtest=ClerkTokenVerifierTest,IdentidadExternaServiceTest,RegistroServiceTest,InvitacionUsuarioServiceTest,InvitacionesServiceIdentidadTest,SuperAdminSeederTest,SeedersSkipOnInitializedDatabaseTest,WriteEndpointsGuardTest,CompanyInitializationTest,AutenticacionServiceTest test`

Resultado: 51 tests, 0 fallos, 0 errores. `mvn test-compile` compila todo el árbol de tests.

Cambios relevantes:

- Puerto `application/port/IdentidadExternaVerificador`; adaptador `ClerkTokenVerifier`.
- `IdentidadExternaService` (login y vinculación por email verificado), `RegistroService.registrarConIdentidad`, `InvitacionUsuarioService.aceptarConIdentidad`, `InvitacionesService.aceptarConIdentidad`.
- `UsuarioEmpresa.conIdentidadExterna` y `vincularIdentidadExterna`.
- `ClerkAuthController` (`/api/auth/clerk/sesion`, `/api/auth/clerk/registro`) y `/api/auth/invitaciones/aceptar-clerk`.
- `LoginConContrasenaPolicy` (`app.auth.password-login.enabled`, `false` por defecto; `true` en `planta-authorization-test`, `e2e-local` y `TeamInvitationsPostgresTest`), `410 LOGIN_CON_CONTRASENA_DESACTIVADO`.
- Eliminados: `ClerkJwtInterceptor`, `ClerkTokenValidator`, `ClerkConfig`, `ClerkUserSyncService`, `InvitacionController` (`/api/invitacion/**`) y sus DTO.
- `SuperAdminSeeder` sin contraseña utilizable cuando el login por contraseña está apagado.

## Pendientes

- Tests HTTP de los endpoints nuevos con un verificador falso y `mvn verify` (requieren aprobación).
- Las columnas `codigo_invitacion` e `invitacion_expira_en` quedan sin uso; eliminarlas requiere una migración aparte.

## Evidencia previa (cambios ya aplicados en esta sesión)

- `ClerkTokenVerifier` (RS256/JWKS, `azp`, email verificado) con `ClerkTokenVerifierTest` 3/3.
- `aceptarClerk` de invitaciones de usuario con identidad verificada; `InvitacionUsuarioServiceTest` 15/15.
- `ClerkUserSyncService` sin altas implícitas; `ClerkUserSyncServiceTest` 2/2 (se reemplazará en la tarea 2).
