# 019 — Estado y evidencia backend

## Estado

- [x] Discovery de autenticación actual, sesión por cookies, invitaciones, registro, super admin y código Clerk previo.
- [x] Decisiones de producto confirmadas por el usuario (2026-11-26): intercambio de sesión, solo Clerk, registro con Clerk, Google, super admin vinculado.
- [x] Spec y plan aprobados por el usuario (2026-11-26). Alcance: implementación BE019/FE041 y tests unitarios sin contexto.
- [ ] Tests HTTP y `mvn verify`: **no autorizados** (2026-11-26); pendientes de aprobación.
- [ ] Implementación.
- [ ] Verificación.
- [ ] Revisión independiente y cierre.

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

## Evidencia previa (cambios ya aplicados en esta sesión)

- `ClerkTokenVerifier` (RS256/JWKS, `azp`, email verificado) con `ClerkTokenVerifierTest` 3/3.
- `aceptarClerk` de invitaciones de usuario con identidad verificada; `InvitacionUsuarioServiceTest` 15/15.
- `ClerkUserSyncService` sin altas implícitas; `ClerkUserSyncServiceTest` 2/2 (se reemplazará en la tarea 2).
