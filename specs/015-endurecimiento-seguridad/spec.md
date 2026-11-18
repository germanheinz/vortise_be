# Spec 015 — Estudio de seguridad y endurecimiento para producción

Estado: **BORRADOR PARA APROBACIÓN**. Revisión estática de código y configuración (no es una prueba de penetración).
Alcance: `gestion_be` (Spring Boot, JWT, JPA) y `gestion_fe` (Next.js 14). Relacionada con [spec 014 (roles)](../014-roles-matriz-accesos/spec.md).

## 1. Método y límites
- Se revisó código y configuración: autenticación, JWT, filtro de seguridad, autorización por endpoint, aislamiento por empresa/obra, CORS, uploads, perfiles de configuración, semillas, frontend (sesión, cabeceras).
- **No se ejecutó** ningún test, análisis de dependencias, escaneo de secretos ni prueba dinámica. Los puntos marcados *verificar* requieren confirmación.
- Referencias de buenas prácticas: OWASP ASVS, OWASP API Security Top 10, OWASP Cheat Sheets (JWT, Session, Authentication, File Upload).

## 2. Lo que ya está bien
| Control | Evidencia |
|---|---|
| Contraseñas con BCrypt y política | `SecurityConfig.passwordEncoder`, `PasswordPolicy` (8–72) |
| Secreto JWT exigido (≥ 32 bytes, sin fallback) | `JwtService` constructor |
| Token máx. 1 h y revocación por `sessionVersion` + `activo` en cada request | `JwtService`, `JwtAuthenticationFilter` |
| Respuestas 401/403 homogéneas y login con mensaje genérico | `SecurityConfig`, `AutenticacionUseCase.login` |
| Aislamiento por empresa/obra en rutas principales | `TenantAccessInterceptor`, `CurrentTenantService` |
| Actuator cerrado salvo `health` y `metrics` (solo `SUPER_ADMIN`) | `SecurityConfig` |
| H2 console deshabilitada; `ddl-auto: validate` + Flyway en producción | `application.yml`, `application-production.yml` |
| Cuota de restablecimiento de contraseña con IP/email hasheados | `PasswordResetTransactions` |
| Orígenes CORS validados por entorno | `WebEnvironmentConfiguration`, `CorsConfig` |
| Webhook de Stripe autenticado por firma, no por JWT | `SecurityConfig` (comentario) — *verificar la validación de la firma* |
| Logging estructurado (logstash) | `application.yml` |
| Límites de tamaño en fotos (2 MB) e imágenes de reporte (5 MB) | `PerfilUseCase`, `ReporteObraUseCase` |

## 3. Hallazgos

Severidad: **Alta** (corregir antes de producción) · **Media** (corregir antes o poco después) · **Baja**.

| ID | Sev. | Hallazgo | Evidencia | Impacto |
|---|---|---|---|---|
| S-01 | Alta | **Escritura sin autorización por rol.** Un usuario `CONSULTA`/`RESPONSABLE` puede modificar datos por API; `JEFE_DE_OBRA` no se limita a su obra | Controladores sin `@PreAuthorize` (ver spec 014 §2) | Pérdida de integridad |
| S-02 | Alta | **Datos globales sin empresa.** `Rendimiento` y `GastoFijo` no tienen empresa: `GetRendimientosUseCase` hace `findAll()` y `RendimientoController` permite `POST/PUT/DELETE` a cualquier autenticado; `GetCashflowUseCase` hace `gastoFijoRepository.findAll()`. `ImportController` también escribe `Rendimiento` global y crea obras sin empresa | `domain/model/Rendimiento`, `GastoFijo`, casos de uso citados | Una empresa puede leer o alterar datos compartidos con otras (fuga y manipulación entre tenants) |
| S-03 | Alta | **Login sin protección contra fuerza bruta**: no hay límite de intentos, bloqueo ni retardo en `login` | `AutenticacionUseCase.login` | Adivinación de contraseñas, credential stuffing |
| S-04 | Alta | **Token en `localStorage`** (`gestion_token`, `gestion_session`) y sin refresh token | `lib/session.ts`, `lib/http-client.ts` | Un XSS roba una sesión válida 1 h |
| S-05 | Media | **Sin cabeceras de seguridad en el frontend** (CSP, `X-Content-Type-Options`, `Referrer-Policy`, `frame-ancestors`, `Permissions-Policy`) | `next.config.mjs` solo define `distDir` | Mayor impacto de XSS/clickjacking |
| S-06 | Media | **Semilla demo por defecto**: `UsuarioDemoSeeder` se activa con `@Profile("!staging & !prod & !production & !e2e-local")` y crea `admin@constructora-norte.demo` / `demo1234`. Además el `application.yml` base usa H2 en archivo, `ddl-auto: update` y `sql.init: always` | `UsuarioDemoSeeder`, `application.yml` | Un despliegue sin perfil expone credenciales conocidas |
| S-07 | Media | **Superficie sin uso**: el frontend solo llama a `gastos-fijos` y `rendimientos`; los endpoints de presupuesto, ingresos, costos indirectos y cashflow existen en el backend sin uso del frontend (gastos fijos/cashflow además están tras un flag `false`) | `lib/api/*`, controladores, `FeaturePolicy` | Endpoints innecesarios ampliando el ataque |
| S-08 | Media | **Cobertura del interceptor por regex** y recursos anidados: solo cubre algunas rutas; en rutas como `/certificados/{c}/items/{rubroId}` no se garantiza que los IDs hijos pertenezcan a la obra de la ruta | `TenantAccessInterceptor` | IDOR por ID anidado |
| S-09 | Media | **Uploads**: solo se verifican tamaño y vacío; *verificar* validación de tipo real (magic bytes), re-codificación y nombres. Límite multipart global 50 MB | `ReporteObraUseCase`, `PerfilUseCase`, `application.yml` | Archivos maliciosos o consumo de memoria |
| S-10 | Media | **Sin auditoría de seguridad**: no se registran 401/403, cambios de rol/obras ni acciones sensibles de forma consultable | — | No hay trazabilidad ni detección |
| S-11 | Media | **Sin MFA** para `ADMINISTRADOR` y `SUPER_ADMIN` | — | Una credencial comprometida da control total |
| S-12 | Media | **Dependencias y secretos sin verificación automática** (*verificar* si existe SCA, `npm audit`, escaneo de secretos en CI) | `pom.xml`, `package.json` | Vulnerabilidades conocidas sin detectar |
| S-13 | Baja | JWT HS256 con secreto único, sin rotación (`kid`) | `JwtService` | Rotar el secreto invalida todas las sesiones |
| S-14 | Baja | CORS `allowedHeaders: *` | `CorsConfig` | Menor superficie si se acota |
| S-15 | Baja | `AGENTS.md` desactualizado (dice Spring Boot 3.2/H2/sin migraciones) | `gestion_be/AGENTS.md` | Decisiones tomadas con información errónea |

## 4. Dónde debe vivir el token (S-04)

**No en `localStorage`.** Recomendación para producción:
- **Cookie `HttpOnly; Secure; SameSite=Lax/Strict`** con un access token corto (≤ 15 min) y un **refresh token rotativo** guardado hasheado en base, revocable y con detección de reutilización. JavaScript no puede leer la cookie, por lo que un XSS no roba el token.
- **Mismo origen:** servir el backend detrás del mismo dominio del frontend (`rewrites` de Next.js o subdominios del mismo sitio). Con orígenes distintos hay que usar `SameSite=None` y CORS con credenciales, lo que complica CSRF.
- **CSRF:** al pasar a cookies, el `csrf.disable()` actual deja de ser suficiente. Usar `SameSite` + verificación de `Origin`/`Referer` o un token CSRF en cabecera para métodos de escritura.
- **Interfaz:** el frontend deja de leer el token. El rol y las obras vienen de `/api/auth/me`; `gestion_session` solo guarda datos no sensibles.
- **Complemento:** una CSP estricta (S-05) reduce el riesgo de XSS aunque el token no sea accesible.
- **Para la demo** puede mantenerse `localStorage` con token de 1 h y CSP estricta, aceptando el riesgo de forma explícita.

## 5. Decisiones del usuario
1. **S-02:** por ahora `Rendimiento` y `GastoFijo` **quedan como están** (catálogo compartido, sin `empresa_id`). Queda como **riesgo aceptado temporalmente**: cualquier `ADMINISTRADOR` de cualquier empresa puede modificar datos compartidos. La spec 014 reducirá la escritura a `ADMINISTRADOR` (filas 21–23); el modelo por empresa se difiere.
2. **S-07:** se **eliminan** los endpoints que el frontend no usa (presupuesto, ingresos, costos indirectos, cashflow y lo que quede huérfano tras el inventario), junto con su código muerto y pruebas asociadas.
3. **S-04:** se **migra** el token a cookie `HttpOnly` antes de producción.
4. **S-11:** **MFA** para `ADMINISTRADOR` y `SUPER_ADMIN`.
5. **S-06:** las semillas de demostración (`UsuarioDemoSeeder`, `DataSeeder`, `DataSeederRunner`, `DemoOperationalSeed`) **no se eliminan**: las pruebas dependen de las clases. Hallazgo verificado: las pruebas HTTP (`Quality14HttpTest`, `PrevistoRealHttpTest`, `MonitoringHttpTest`, `RubroEstructuraPersistenciaTest`, `TeamInvitationsContract`, etc.) las declaran con `@MockitoBean` para desactivarlas, y hay pruebas unitarias propias (`DataSeederTest`, `DemoOperationalSeedTest`, `UsuarioDemoSeederTest`, `DemoSeederCoverageTest`) y de configuración (`ProductionConfigurationTest`, `SeedersSkipOnInitializedDatabaseTest`). En su lugar se **restringe su activación a opt-in explícito**: `UsuarioDemoSeeder` pasa de `@Profile("!staging & !prod & !production & !e2e-local")` a un perfil explícito (`demo`/`local`), y se comprueba que `DataSeederRunner` quede gobernado por la misma regla. Se conservan `PlanSuscripcionSeeder`/migración V2 y `SuperAdminSeeder`. El endpoint `reset-demo` se mantiene, ya deshabilitado por `app.demo.reset-enabled=false` y limitado a `ADMINISTRADOR`; se revisa su retiro más adelante.

## 6. Criterios de aceptación
1. Ninguna escritura accesible a roles de solo lectura; el test de enumeración de endpoints exige guard (spec 014).
2. Ningún dato de una empresa es legible ni modificable por otra (pruebas cruzadas, incluidos `Rendimiento`, `GastoFijo` e importación).
3. El login limita intentos por cuenta e IP, con respuesta uniforme y registro del evento.
4. El token no es accesible desde JavaScript (si se aprueba S-04) y existen CSP y demás cabeceras en el frontend.
5. Un despliegue sin perfil explícito no crea usuarios demo ni usa H2.
6. Los endpoints sin uso quedan retirados o protegidos.
7. Existen pruebas de IDOR en rutas anidadas y de validación de uploads.
8. Auditoría de 401/403, cambios de rol/obras y acciones sensibles.
9. CI con escaneo de dependencias y de secretos, sin vulnerabilidades altas abiertas.
10. Revisión independiente (security-review) cerrada.

## 7. Riesgos del plan
- Mover el token a cookies exige coordinar frontend y backend y cambia el CORS y CSRF.
- Agregar `empresa_id` a `Rendimiento`/`GastoFijo` requiere migración y backfill con aprobación.
- El límite de intentos de login puede bloquear a usuarios legítimos: definir ventana, umbrales y desbloqueo.
- Todo cambio de datos y toda prueba HTTP respetan las aprobaciones de ejecución del `AGENTS.md` (base efímera, nunca el H2 persistente).
