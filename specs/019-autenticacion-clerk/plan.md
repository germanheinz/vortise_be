# 019 — Plan técnico backend

## Estado

Aprobado por el usuario el 2026-11-26 para implementación. Tests HTTP, `mvn verify`, `tsc` y `next build` no autorizados en este hito. Requisitos en [spec](spec.md); consumidor en [plan frontend 041](../../../gestion_fe/specs/041-autenticacion-clerk/plan.md).

## Arquitectura

Clerk es un detalle de infraestructura detrás de un puerto de aplicación. Los casos de uso no conocen Clerk ni HTTP.

```
controller/authentication/ClerkAuthController ──┐
controller/authentication/InvitacionPublicaController ─┤  extraen el Bearer (BearerToken) y llaman al puerto
                                                 ▼
application/port/IdentidadExternaVerificador  ◄── infrastructure/security/clerk/ClerkTokenVerifier (adaptador)
        │ IdentidadVerificada(sujeto, email)
        ▼
service/authentication/IdentidadExternaService   (login: buscar por sujeto → vincular por email → activo)
service/authentication/RegistroService           (registrarConIdentidad)
service/InvitacionUsuarioService                 (aceptarConIdentidad)
service/InvitacionesService                      (aceptarConIdentidad)
        │ LoginResponseDto
        ▼
service/authentication/SesionService.abrir → SessionCookies (sin cambios)
```

## Decisiones técnicas

1. **Puerto `IdentidadExternaVerificador`** en `application/port`, con `IdentidadVerificada verificar(String token)` y el record `IdentidadVerificada(String sujeto, String email)`. `ClerkTokenVerifier` lo implementa (RS256 con JWKS cacheado, `exp`/`nbf` con 5 s de tolerancia, `azp` en orígenes permitidos, email principal verificado vía Backend API). Inversión de dependencias: los servicios dependen de la abstracción.
2. **`IdentidadExternaService`** (nuevo, responsabilidad única): resuelve un usuario a partir de una identidad verificada. Reemplaza `ClerkUserSyncService` (se elimina). Reglas: buscar por `clerkUserId`; si no, por email verificado sin vínculo previo o con el mismo; exigir `activo`; nunca crear usuarios. Excepción `CuentaNoRegistradaException` → `403 CUENTA_NO_REGISTRADA`.
3. **Altas con identidad**: `RegistroService`, `InvitacionUsuarioService` e `InvitacionesService` reciben `IdentidadVerificada` en lugar de contraseña. Un único método de fábrica `UsuarioEmpresa.conIdentidadExterna(...)` encapsula hash local no utilizable, `clerkUserId`, `tieneLogin` y `migradoAClerk` (evita repetir esa lógica en tres servicios).
4. **Extracción del Bearer**: helper `BearerToken.from(HttpServletRequest)`/`@RequestHeader` en un solo lugar; los controllers no parsean headers a mano.
5. **Flag `app.auth.password-login.enabled`** (por defecto `false`). Un `PasswordLoginGuard` en los endpoints de contraseña responde `410` con código estable cuando está apagado. Los perfiles de test (HTTP y `e2e-local`) lo activan para conservar las fixtures existentes. Mapeado en `GlobalExceptionHandler` con `PasswordLoginDisabledException`.
6. **Eliminar código de identidad no verificada**: `ClerkJwtInterceptor`, `ClerkTokenValidator`, `InvitacionController` (`/api/invitacion/**`), sus DTO y su registro en `SecurityConfig`; `clerkUserId` deja de aceptarse en cualquier body.
7. **Super admin**: `SuperAdminSeeder` guarda un hash aleatorio no utilizable cuando el login por contraseña está desactivado; conserva email y flag `superAdmin`.
8. **Configuración**: `app.clerk.enabled`/`app.clerk.secret-key` desde `CLERK_ENABLED`/`CLERK_SECRET_KEY` (ya agregado). Sin secretos en el repositorio.
9. **Persistencia**: sin cambios de esquema; las columnas `clerk_user_id`, `tiene_login` y `migrado_a_clerk` ya existen. Los campos `codigo_invitacion`/`invitacion_expira_en` quedan sin uso; su eliminación se difiere para no requerir migración.
10. **Logging**: solo ids internos; nunca token, secreto ni email completo.

## Pruebas

- Unitarias (sin Spring ni base): `ClerkTokenVerifier` (ya existe), `IdentidadExternaService`, registro e invitaciones con identidad, guard de contraseña.
- HTTP (Spring + H2 efímera, requiere aprobación de ejecución): nuevos endpoints con un `IdentidadExternaVerificador` falso en el contexto de test; `410` con flag apagado.
- `mvn verify` final, con aprobación.

## Riesgos y limitaciones

- Una sesión del backend sigue viva hasta su vencimiento aunque Clerk revoque la suya; el logout cierra ambas.
- Primera vinculación depende de la disponibilidad de la API de Clerk (`503` si no responde).
- Los usuarios existentes deben verificar su email en Clerk antes del primer ingreso.
