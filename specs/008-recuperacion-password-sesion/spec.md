# 008 — Recuperación de contraseña y sesión segura

## Estado

Implementación completa aprobada explícitamente el 2026-10-28 para BE008/FE019. Verificación aislada autorizada: unitarias, HTTP en H2 efímera con guard, mvn verify tras auditoría de contextos, tsc y build FE; correcciones/reintentos dentro del mismo aislamiento. SMTP configurable aprobado, proveedor y activación remota diferidos. No autoriza publicación, despliegue, migración remota, datos persistentes ni envíos reales en este hito.

## Objetivo

Permitir que una persona recupere su contraseña por email y asegurar que tokens expirados, usuarios desactivados y roles modificados no mantengan acceso obsoleto. Mantener el login simple con email y contraseña; no incorporar OAuth, MFA ni refresh tokens en esta fase.

## Alcance

1. Mantener BCrypt para contraseñas y las reglas actuales de longitud de registro al establecer una nueva contraseña.
2. Solicitar recuperación sin revelar si una dirección existe. Para una cuenta existente, enviar un enlace de un solo uso, válido por 30 minutos como máximo.
3. Guardar únicamente el hash del token aleatorio; consumirlo atómicamente una vez y rechazarlo si expiró, ya se usó o fue reemplazado.
4. Al completar el cambio, actualizar el hash BCrypt, invalidar sesiones JWT previas y enviar una notificación de seguridad al correo de la cuenta.
5. Responder `401` para autenticación ausente, inválida o expirada; conservar `403` para una identidad autenticada sin permiso.
6. Verificar en cada solicitud protegida que el usuario siga activo y que la versión de sesión/credenciales del JWT siga vigente. Derivar los roles y `superAdmin` del usuario actual, no confiar en claims de autorización obsoletos.
7. Conservar los límites de empresa y proyecto actuales. Los roles del frontend solo controlan presentación; la autorización efectiva siempre es backend.
8. La interfaz cliente elimina la sesión y vuelve a login ante `401`; no debe cerrar sesión ante `403`.

## Contrato API

- `POST /api/auth/password-reset/request`, cuerpo `{ "email": string }`, responde siempre `202 Accepted` con el mismo mensaje público, exista o no la cuenta.
- `POST /api/auth/password-reset/confirm`, cuerpo `{ "token": string, "newPassword": string }`, responde `204 No Content` al consumirse correctamente. Token inválido, usado o expirado produce un error estable sin filtrar información de cuentas.
- Los endpoints no devuelven usuario, estado de existencia, token, contraseña ni secretos.
- El enlace usa el origen frontend configurado para el entorno. El token no se escribe en logs y se retira de la barra de direcciones al cargar el formulario.

## Criterios de aceptación

1. Una solicitud válida genera un único token criptográficamente aleatorio, almacena solo su hash y envía el enlace solo a una cuenta existente.
2. La respuesta pública es idéntica para email existente e inexistente; se aplican límites/cooldown contra abuso sin bloquear el login normal.
3. Un token puede consumirse una sola vez, expira en 30 minutos o menos y un token nuevo invalida el anterior.
4. La contraseña nueva se guarda con BCrypt y cumple la política existente; nunca se registra en logs.
5. Al confirmar el reset, tokens anteriores dejan de autenticar inmediatamente y llega una notificación de cambio sin incluir la contraseña.
6. Token ausente, malformado, de firma incorrecta o expirado resulta en `401`; usuario autenticado sin autorización resulta en `403`.
7. Desactivación, cambio de rol o `superAdmin` invalidan la sesión anterior; la autorización backend usa los valores vigentes de la cuenta.
8. El usuario ve la página de login tras `401`; un `403` conserva su sesión y muestra acceso denegado.
9. Los tests cubren enumeración, emisión/consumo, expiración, reutilización, invalidación, roles, usuario inactivo y separación `401`/`403` en un entorno de prueba aislado.
10. La pantalla de login no prellena ni publica credenciales demo en builds públicos.

## Fuera de alcance

- OAuth/OIDC, MFA/passkeys, refresh tokens, sesiones administrables o dispositivos confiables.
- Envíos a cuentas reales durante pruebas; pruebas de entrega solo con sandbox/buzón controlado.
- Cambios de política comercial o de Stripe.
- Migración inmediata de bearer JWT a cookie HttpOnly; se registra como riesgo de seguridad separado para evaluación.
