# Tasks 015 — Endurecimiento de seguridad

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)
Estado global: **pendiente de aprobación**. Nada implementado.

| ID | Fase | Tarea | Depende de | Estado | Criterio |
|---|---|---|---|---|---|
| T-001 | H1 | Estudio y plan de seguridad | — | Hecho | spec.md y plan.md |
| T-002 | H2 | Decisiones (S-02, S-07, S-04, S-11, S-06) y aprobación | T-001 | Pendiente | Decisiones registradas |
| T-010 | S0 | F0 de spec 014 (filtro de solo lectura) | T-002 | Implementado; verificación HTTP pendiente | `CONSULTA`/`RESPONSABLE` reciben 403 en escrituras |
| T-011 | S0 | Documentar riesgo aceptado de S-02; escritura de `rendimientos`/`gastos-fijos` a `ADMINISTRADOR` (con guards de la spec 014) | T-002 | Implementado; verificación HTTP pendiente | Otros roles 403; riesgo registrado |
| T-012 | S0 | Límite de intentos de login por cuenta e IP | T-002 | Implementado; verificación HTTP pendiente | Bloqueo temporal, respuesta uniforme, evento registrado |
| T-013 | S0 | Semillas demo con activación opt-in (perfil explícito); sin borrar clases ni pruebas | T-002 | Implementado (pruebas sin contexto verdes) | Sin perfil explícito no se crea ningún usuario/credencial demo; pruebas de semillas, `ProductionConfigurationTest` y `SeedersSkipOnInitializedDatabaseTest` verdes |
| T-020 | S1 | Spec 014 F1–F3 (token con rol/obras, guards, gestión de usuarios) | T-010 | Implementado; verificación HTTP pendiente | Criterios de la spec 014 |
| T-021 | S1 | Diferido: modelo por empresa de `Rendimiento`/`GastoFijo` (revisar antes de abrir a más empresas reales) | T-002 | Diferido | Decisión registrada |
| T-022 | S1 | Eliminar endpoints, casos de uso, repositorios y pruebas sin uso del frontend | T-002 | Hecho (salvo `PlantaController`, ver evidencia) | Lista de eliminados; build y pruebas verdes |
| T-023 | S1 | Inventario de rutas fuera del interceptor y pruebas de IDOR anidado | T-020 | Revisión hecha; pruebas HTTP de IDOR pendientes | Pruebas por ruta verdes |
| T-024 | S1 | Auditoría de seguridad (log estructurado) | T-020 | Implementado (`AuditLog`, 4 pruebas) | Eventos definidos y probados |
| T-030 | S2 | Cookie `HttpOnly` + access corto + refresh rotativo + CSRF (aprobado) | T-020 | Implementado; verificación en navegador pendiente | Token no accesible desde JS |
| T-031 | S2 | Proxy/`rewrites` para mismo origen | T-030 | Implementado (rewrites de Next); no probado contra un backend real | Sin CORS con credenciales entre orígenes |
| T-032 | S2 | Cabeceras de seguridad y CSP en el frontend | T-002 | Pendiente | Cabeceras presentes; build sin violaciones |
| T-033 | S2 | Acotar CORS; evaluar `kid` | T-030 | Pendiente | Configuración revisada |
| T-040 | S3 | Hardening de uploads (magic bytes, re-codificación, límites) | T-002 | Pendiente | Pruebas con archivos inválidos |
| T-041 | S3 | CI: dependencias y secretos (backend y frontend) | T-002 | Pendiente | Sin vulnerabilidades altas abiertas |
| T-042 | S3 | MFA TOTP para `ADMINISTRADOR`/`SUPER_ADMIN` (aprobado) | T-030 | Pendiente | Login con segundo factor probado |
| T-043 | S3 | Verificar firma del webhook de Stripe; actualizar `AGENTS.md` | T-002 | Pendiente | Verificado y documentado |
| T-050 | S4 | Verificación completa por repositorio | T-024, T-032, T-040 | Pendiente | Resultados reales reportados |
| T-051 | S4 | Revisión de seguridad independiente y cierre | T-050 | Pendiente | Hallazgos resueltos o aceptados |

## Aprobaciones de ejecución a solicitar (agrupadas)
1. Pruebas enfocadas sin contexto.
2. Pruebas HTTP con Spring + H2 efímero (incluidas migraciones sobre base vacía).
3. `mvn verify`, `tsc` y `npm run build`.
4. Herramientas de análisis de dependencias/secretos (solo lectura).
No se autoriza nada sobre el H2 persistente, despliegues ni escrituras remotas sin aprobación adicional.

## Evidencia

### S0 — 2026-11-18 (gestion_be)
**Cambios:** `SoloLecturaWriteGuard` (+ registro en `SecurityConfig`, propiedad `app.security.read-only-write-guard.enabled`, por defecto `true`); `@PreAuthorize("hasRole('ADMINISTRADOR')")` en escrituras de `RendimientoController` y `GastoFijoController`; `LoginThrottle` (reutiliza `password_reset_quotas`, claves `lc:`/`li:` con hash, sin migración), `DemasiadosIntentosException` → 429 con `Retry-After`, `PasswordResetQuota.reiniciar`; `AutenticacionUseCase.login(request, ip)`; `SeedProfiles.DEMO_ONLY` aplicado a `UsuarioDemoSeeder` y `DataSeederRunner`. Pruebas HTTP existentes ajustadas: `PlantaAuthorizationHttpTest` (roles de solo lectura excluidos de las pruebas de escritura y nueva `readOnlyRolesAreDeniedBeforeAnyLookup`) y `PrP0SecurityHttpTest` (presupuesto PUT/DELETE con `JEFE_DE_OBRA`).

**Ejecutado (sin contexto de Spring ni base de datos):**
`mvn -q -o -Dtest='SoloLecturaWriteGuardTest,LoginThrottleTest,AutenticacionUseCaseTest,SeedProfilePolicyTest,ProductionConfigurationTest,UsuarioDemoSeederTest,SeedersSkipOnInitializedDatabaseTest,DataSeederTest,GastoFijoAvailabilityTest,JwtAuthenticationFilterTest' -Dsurefire.failIfNoSpecifiedTests=false test`
Resultado (informes surefire): 63 pruebas, 0 fallos, 0 errores, 0 omitidas. Esta ejecución también compiló todo el código y las pruebas, incluidas las HTTP modificadas.

**No ejecutado:** las 11 pruebas HTTP con H2 en memoria (`PlantaAuthorizationHttpTest`, `PrP0SecurityHttpTest`, `Quality14HttpTest`, `PasswordResetHttpTest`, `TeamInvitationsHttpTest`, `MonitoringHttpTest`, `PrevistoRealHttpTest`, `RestriccionDetalleHttpTest`, `RubroEstructuraPersistenciaTest`, `SuperAdminDashboardHttpTest`, `H2LogicalBackupHttpTest`): el usuario no autorizó su ejecución. Tampoco las pruebas Postgres/Testcontainers, `mvn verify`, `tsc` ni `npm run build`. El comportamiento del filtro y del límite de login en la cadena real de Spring Security **no está verificado**.

**Notas y límites:**
- El límite por IP usa `getRemoteAddr()`; detrás de un proxy inverso (y con el proxy de Next.js de S2) todos los usuarios compartirían IP. Por eso el límite por IP es amplio (50 fallos/15 min) y el de cuenta es 5 fallos/15 min. Configurar `server.forward-headers-strategy` con proxies de confianza antes de producción.
- `DatabaseInitializationPolicy`, `AdminBackupController` y `H2LogicalBackupService` mantienen la regla antigua (`!staging & !prod & ...`), por lo que siguen activos sin perfil explícito. Quedan fuera de T-013; revisarlos en S1.
- Sin un perfil explícito (`demo`, `local`, `payments-local` o `planta-authorization-test`) ya no se siembran datos demo: el arranque local con H2 ahora requiere `-Dspring-boot.run.profiles=demo`.

### S1 — 2026-11-18 (gestion_be)
**T-022 — endpoints eliminados** (el frontend nunca los llama): `EntregaController`, `TareaController`, `RendimientoController`, `IngresoController`, `CostoIndirectoController`, `PresupuestoLineaController`, `CashflowController` y `DashboardController`, con 15 casos de uso exclusivos, 10 DTO, `CashflowCalculadorService` y 3 pruebas dedicadas (`CashflowFeatureAvailabilityTest`, `DashboardTenantAuthorizationTest`, `PresupuestoLineaOwnershipTest`); en `PrP0SecurityHttpTest` se quitaron las pruebas de dashboard, presupuesto y cashflow. Se conservan entidades, repositorios y tablas (los usan las semillas y `ExcelImportService`). **No se eliminó `PlantaController`** (el frontend tampoco lo llama) porque `AGENTS.md` mantiene protegida su suite de autorización: pendiente de decisión. La tarea T-011 sobre `RendimientoController` quedó sin objeto al eliminarse el controlador; `GastoFijoController` conserva su guard.

**T-023 — revisión de IDs anidados:** los casos de uso ya validan que el hijo pertenece a la obra de la ruta (`certificados.findByIdAndProyectoId`, `reportes.findByIdAndProyectoId` / `findByIdAndProyectoId` de imágenes, `ObraTarea` en `update` y `delete`, `Planta`); restricciones y registro de horas se resuelven por su obra en `TenantAccessInterceptor`. Faltan pruebas HTTP por ruta.

**T-024 — auditoría:** `AuditLog` (logger `AUDIT`) registra `access_denied` (códigos `ACCESO_SOLO_LECTURA`, `CAPACIDAD_DENEGADA`, `OBRA_NO_ASIGNADA`, `REGLA_DE_ESTADO`) y `user_access_changed`; sin emails, contraseñas ni cuerpos; limpia saltos de línea y trunca valores.

**Verificación ejecutada (sin Spring ni base de datos):**
- `mvn -o -Dtest='!<16 clases @SpringBootTest>' -Dsurefire.failIfNoSpecifiedTests=false test`: **495 pruebas, 1 fallo, 0 errores**.
- El fallo es `CreateRestriccionUseCaseTest.rejectsNewRestrictionAfterProjectUploadDeadline`: **preexistente y ajeno a estos cambios**. `CreateRestriccionUseCase` no valida el plazo de carga de restricciones en el código commiteado (no está modificado) y la prueba espera `SolicitudInvalidaException`. Pendiente de decisión del usuario.
- `python3 -m unittest test_extract_empresas` (scripts/tests): 17 pruebas OK, con `usuario_obra` clasificada.
- `StagingSeedRunPolicyTest` se actualizó para fijar `SeedProfiles.DEMO_ONLY` (cambio de S0).

**No ejecutado:** las 11 pruebas HTTP con H2 en memoria (ampliadas en S1: `RestriccionDetalleHttpTest`, `PrevistoRealHttpTest` y `RubroEstructuraPersistenciaTest` pasaron a token `ADMINISTRADOR`; `PlantaAuthorizationHttpTest` ahora cubre `ADMINISTRADOR`/`COLABORADOR` y que `JEFE_DE_OBRA` recibe 403), la migración V22 contra una base real, `mvn verify`, `tsc` y `npm run build`.

### S2 — token en cookie HttpOnly (2026-11-18)
**Backend:** `V23__refresh_tokens.sql` (aditiva; solo guarda el hash) y `RefreshToken`; `RefreshTokenService` rota el token en cada uso, revoca toda la familia si se reutiliza uno retirado (con 10 s de gracia para pestañas concurrentes), lo invalida si cambia `sessionVersion` o la cuenta se desactiva, y limita la cadena a 30 días; `SesionUseCase` y `SessionCookies` entregan `gestion_at` (acceso, 15 min, `Path=/api`) y `gestion_rt` (refresh, 7 días, `Path=/api/auth`), ambas `HttpOnly; Secure; SameSite=Strict`; `AuthController` agrega `/api/auth/refresh` y `/api/auth/logout`, y login, registro e invitación ya **no devuelven el token en el cuerpo**; `JwtAuthenticationFilter` lee la cookie (el header Bearer sigue aceptado para clientes de API); `CookieOriginGuard` exige un `Origin` o `Referer` permitido en toda escritura que lleve cookie de sesión (CSRF); el token de acceso pasa de 1 h a 15 min; `app.security.cookie.secure` (por defecto `true`, `false` en perfiles local/e2e); `refresh_tokens` clasificada en `extract-empresas.py`; `AuditLog` registra la reutilización de un refresh token.
**Frontend:** `next.config.mjs` reenvía `/api` al backend (`API_PROXY_TARGET`, o `NEXT_PUBLIC_API_URL`) y el cliente usa el mismo origen; `localStorage` ya no guarda el token (solo el perfil no sensible); el cliente renueva la sesión una vez ante un 401 y reintenta la petición, con un único refresh compartido; `authApi.logout()` en los dos puntos de cierre de sesión; el e2e y su launcher se adaptaron (no ejecutados).
**Ejecutado:** backend sin Spring 535 pruebas, 0 fallos; 12 clases HTTP con H2 en memoria autorizadas por el usuario: `CookieSessionHttpTest` (9 pruebas: login, cookie, origen, rotación, reutilización, cambio de sesión, logout, Bearer) pasa; las otras 11 clases dan los mismos 11 fallos preexistentes verificados contra `HEAD` (3 de entrada mal formada que devuelve 500, 8 de `H2LogicalBackupHttpTest`); frontend 251 pruebas de `node --test` OK (incluye 13 de sesión reescritas), `tsc` sin errores en código fuente y `npm run build` OK en el reintento (el primer intento se detiene con un aviso de actualización de Next.js).
**No ejecutado / pendiente:** el e2e de Playwright (necesita servidores, PostgreSQL desechable y navegador); la migración V23 contra PostgreSQL; una prueba real del proxy de Next con un backend; cabeceras de seguridad y CSP (T-032), MFA (T-042) y acotar CORS (T-033).
**Antes de producción:** definir `API_PROXY_TARGET` (o `NEXT_PUBLIC_API_URL`) en el despliegue del frontend; `APP_ALLOWED_ORIGINS` debe incluir el origen público del frontend; verificar que el proxy del hosting admita las subidas multipart (fotos 2 MB, imágenes de reporte 5 MB); las sesiones abiertas antes del cambio se cierran (no hay refresh token) y el usuario debe volver a iniciar sesión.
