# 019 — Autenticación con Clerk

## Estado

Aprobado por el usuario el 2026-11-26 para implementación. Tests HTTP, `mvn verify`, `tsc` y `next build` no autorizados en este hito. Decisiones de producto confirmadas por el usuario; ver [plan](plan.md). Contrato consumidor: [SDD frontend 041](../../../gestion_fe/specs/041-autenticacion-clerk/spec.md).

## Objetivo

Delegar en Clerk la gestión de credenciales (alta, login, verificación de email, recuperación de contraseña y Google) y mantener en el backend la autorización y la sesión propia (cookies HttpOnly). El backend nunca confía en identidades enviadas por el cliente: solo en un token de sesión de Clerk verificado y en el email principal que Clerk informa como verificado.

## Decisiones confirmadas

1. **Modelo de sesión: intercambio.** El navegador obtiene un token de sesión de Clerk y lo presenta una vez a un endpoint del backend, que abre la sesión habitual (`SesionService`, cookies de acceso y refresh). El resto de la API no cambia.
2. **Solo Clerk.** El login por contraseña del backend se desactiva. Los usuarios existentes entran con Clerk usando el mismo email (creando la cuenta o con "Olvidé mi contraseña") y quedan vinculados por email verificado.
3. **Registro público con Clerk.** La cuenta se crea en Clerk (email verificado); después el backend crea la empresa y su administrador con ese email.
4. **Google habilitado** en login, registro e invitaciones, con las mismas reglas de email verificado.
5. **Super admin** se vincula por email verificado como cualquier usuario; el seeder no deja una contraseña utilizable.

## Alcance

1. Iniciar sesión con una cuenta Clerk vinculada a un usuario activo.
2. Vincular automáticamente, en el primer login, una cuenta Clerk con un usuario existente cuyo email coincide con el email principal verificado, salvo que ese usuario ya esté vinculado a otra cuenta Clerk.
3. Registrar empresa + administrador con identidad Clerk verificada.
4. Aceptar invitaciones de usuario (rol fijo) y de equipo (colaborador) con identidad Clerk verificada cuyo email coincida con el invitado.
5. Desactivar login, registro, recuperación de contraseña y aceptación de invitaciones con contraseña del backend, con un flag de configuración que solo los perfiles de test pueden activar.
6. Eliminar las rutas de identidad no verificada: `ClerkJwtInterceptor` (Bearer de Clerk por request), `ClerkTokenValidator` (inerte) y `/api/invitacion/**` (vincula un `clerkUserId` sin verificar).
7. Mantener sin cambios: autorización por empresa/obra/rol, `sessionVersion`, desactivación de usuarios, refresh y logout del backend.

## Contrato API

Todas las operaciones Clerk reciben `Authorization: Bearer <token de sesión de Clerk>` y responden con las cookies de sesión del backend y el mismo `LoginResponseDto` que hoy.

| Método y ruta | Cuerpo | Éxito | Errores |
|---|---|---|---|
| `POST /api/auth/clerk/sesion` | — | `200` | `401` token inválido; `403` `CUENTA_NO_REGISTRADA` (cuenta Clerk sin usuario); `403` usuario inactivo o vinculado a otra cuenta |
| `POST /api/auth/clerk/registro` | `{ empresaNombre, nombre }` | `201` | `401`; `400` con mensaje genérico si el email, la cuenta o la empresa ya existen (igual que el registro actual, sin revelar cuál) |
| `POST /api/auth/invitaciones-usuario/aceptar-clerk` | `{ codigo, nombre }` | `201` | `401`; `400` email distinto o invitación no disponible; `409` cuenta existente o límite |
| `POST /api/auth/invitaciones/aceptar-clerk` | `{ token, nombre }` | `201` | `401`; `400` invitación no disponible; `409` cuenta existente o cupo. La invitación de equipo no está atada a un email: se usa el email verificado de la cuenta. |

Con el flag de contraseña desactivado, `POST /api/auth/login`, `/auth/registro`, `/auth/password-reset/*`, `/auth/invitaciones/aceptar` y `/auth/invitaciones-usuario/aceptar` responden `410 Gone` con código `LOGIN_CON_CONTRASENA_DESACTIVADO`.

`POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/me` y los endpoints de consulta de invitaciones no cambian.

## Criterios de aceptación

1. Un token de Clerk ausente, malformado, vencido, firmado con otra clave, emitido para un origen no permitido o con sujeto distinto de `user_*` produce `401` y no abre sesión.
2. Un usuario vinculado y activo obtiene cookies de sesión con su empresa, rol y `superAdmin` actuales.
3. Una cuenta Clerk sin usuario local recibe `403 CUENTA_NO_REGISTRADA`; nunca se crea un usuario implícitamente.
4. La vinculación por email requiere email principal verificado en Clerk y no reemplaza una vinculación existente con otra cuenta.
5. El registro crea exactamente una empresa y un administrador con el email verificado y el nombre del formulario; si el email, la cuenta o el nombre de empresa ya existen responde `400` genérico sin crear nada.
6. Aceptar una invitación exige que el email verificado sea el invitado; asigna empresa, rol y obras de la invitación; consume la invitación una sola vez y respeta el límite de usuarios.
7. Un usuario inactivo no puede abrir sesión con Clerk.
8. Con el flag desactivado, ningún endpoint acepta contraseñas; con el flag activo en tests, los flujos actuales siguen funcionando.
9. Ningún log contiene tokens, secretos ni emails completos.
10. El super admin sembrado no tiene una contraseña local utilizable y se vincula por email verificado.

## Fuera de alcance

- MFA, passkeys, organizaciones de Clerk y gestión de sesiones/dispositivos.
- Revocar sesiones del backend cuando Clerk revoca la suya (la sesión del backend vence por sí sola o con logout).
- Sincronizar cambios de email hechos en Clerk hacia el usuario local.
- Webhooks de Clerk.
- Despliegue, configuración del panel de Clerk (Google, plantillas de email) y migración de datos remotos.
