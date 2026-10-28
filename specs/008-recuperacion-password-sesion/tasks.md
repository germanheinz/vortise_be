# 008 — Estado y evidencia backend

## TODO — Correo de recuperación

- [ ] Configurar un proveedor gratuito y un remitente autorizado, con transporte compatible con el hosting.
- [ ] Activar el envío y verificar con un buzón real controlado la recepción del enlace, el consumo único del token y su vencimiento.

El usuario decide dejar este punto como TODO. Mientras el correo esté deshabilitado, la solicitud responde202 genérico pero no emite token ni envía un enlace. Implementación local verificada; entrega real pendiente. Referencia: [activación de correo](smtp-activation.md). Sin cambios de configuración ni envíos por esta decisión.

## Estado

- [x] Discovery estático de login, JWT, filtro, roles, DTO y manejo de errores.
- [x] Propuesta conceptual aprobada por el usuario el 2026-10-27.
- [x] Contrato y plan técnico redactados como borrador.
- [x] SDD aprobado; SMTP configurable con proveedor diferido y cuotas persistentes concretadas.
- [x] Aprobación explícita de archivos y alcance de implementación (2026-10-28).
- [x] Endpoints, entidades, V8, sesión vigente y SMTP configurable implementados.
- [x] Unitarias/HTTP, mvn verify y PostgreSQL desechable autorizados y ejecutados; evidencia abajo.
- [x] Revisión independiente y verificación local aprobadas; activación SMTP/despliegue quedan fuera de este cierre local.

## Evidencia observada

- `JwtService` configura por defecto 86.400.000 ms (24 h) y emite claims `userId`, `empresaId`, `rol` y `superAdmin`.
- `JwtAuthenticationFilter` parsea el JWT; ante error limpia el contexto y continúa. Construye authorities desde claims del token.
- `PlantaAuthorizationHttpTest` caracteriza tokens ausentes, malformados, expirados y con firma incorrecta como `403`; el plan propone cambiar esos casos a `401`, conservando `403` para denegaciones de autorización.
- `SecurityConfig` permite `/api/auth/**`, usa sesión stateless y no configura entry point REST explícito.
- Login valida usuario activo y BCrypt. `UsuarioEmpresa` tiene hash BCrypt, rol y `activo`, pero aún no versión de sesión.
- No se encontró integración/dependencia de correo.
- El entorno local usa H2 persistente; no se ejecutó ni se consultó la base.

## Verificación

No se modificó código backend ni se ejecutaron Maven, pruebas, migraciones, llamadas de correo, Stripe o despliegues. Las verificaciones propuestas requieren aprobación delimitada conforme a `gestion_be/AGENTS.md`.

## Seguridad pendiente

Credenciales adjuntas previamente al chat deben rotarse por el dueño antes de usar staging. No copiar valores de `.env` a este registro.

## Aprobación y alcance — 2026-10-28

Usuario confirma «Implementar recuperación y sesiones seguras (BE008/FE019)», autoriza verificación aislada descrita y elige SMTP con proveedor posterior. Alcance local: modelos/repositorios/usecases/JWT/filtro/controladores, SMTP y pom, V8 y schema H2, tests y sus fixtures; FE api/login/layout/rutas de recuperación y verificaciones. Una lectura de usuario por solicitud protegida; JWT previos sin versión requieren nuevo login. Datos H2 preexistentes excluidos y no consultados. Sin cambios remotos ni secretos nuevos.

## Implementación y verificación — 2026-10-28

Ramas locales independientes: BE `feature/be008-password-session`, FE `feature/fe019-password-session`. No commits/push/deploy ni cambios remotos. H2 persistente preexistente excluida, sin leerla/restaurarla. SMTP permanece deshabilitado por defecto; proveedor expresamente diferido. Usuario autoriza PG desechable15434 y elige solo tests/build, sin navegador ni servidor frontend.

Backend: nuevos endpoints request202 y confirm204; token aleatorio32bytes, solo SHA256 persistido, vencimiento30min y un token por cuenta. Consumo bajo lock de usuario con routing escalar (sin cargar una entidad stale antes del lock), probado concurrentemente en H2 y PostgreSQL. Versión de sesión en usuario/JWT, account lookup vigente y roles/empresa/superAdmin actuales; contraseña, rol y actividad invalidan tokens previos. JWT máximo1h y tokens heredados sin versión rechazados. 401 autenticación /403 autorización. BCrypt y límite72bytes; SMTP STARTTLS con timeouts y executor acotado, mensajes de error sanitizados sin credenciales/tokens. Cuotas persistentes global/email/IP, cleanup2h; no se confía en X-Forwarded-For arbitrario. Tokens/cuotas no se exportan en backupH2; restore invalida links y eleva versiones de sesión. Test adicional cubre restauración de snapshot anterior sin reactivar JWT.

Frontend: rutas públicas recuperar/restablecer-password, fragmento del enlace capturado solo en memoria y retirado de URL; formularios con loading/error/success y sin almacenamiento de contraseña/token reset. Login vacío sin texto demo, enlace recuperar y retorno local validado. Interceptor protegido401 borra sesión una sola vez;403/red/5xx conservan sesión. Tests Node con VM ejecutan el código real y sustituyen solo axios/browser, sin nueva dependencia.

Comandos y resultados observados:
- `tsc --noEmit --incremental false`: código0. `NEXT_PUBLIC_API_URL=https://vortise-be.onrender.com npm run build`: código0, incluye ambas rutas nuevas. Repetidos tras corrección del interceptor y exitosos.
- `node --test tests/session-safety.test.cjs`: **5 pruebas,5 pasan**, sin red. Casos:401 simultáneos,403/red/5xx, URL inválida preserva error, rutas/endpoints públicos y retorno externo/backslash/control rechazado.
- Primer `mvn -DskipTests test-compile` bloqueado por escritura en caché Maven; reintento autorizado descargó SpringMail. Fallo de compilación por test del constructor antiguo corregido.
- Primera suite focalizada **16 pruebas,1 fallo** de codificación UTF8 en la expectativa JSON, sin errores; fixture corregida usando UTF8 explícito. Reintento **16/16 pasan**.
- Primer `mvn verify`: **388 pruebas contabilizadas,4 fallos,0 errores,3 omitidas**. Dos expectativas heredadas de excepción IllegalArgumentException sustituidas por BadCredentialsException; dos JWT sin versión/identidad ahora401 en lugar de409, conforme al contrato aprobado. Regresión focalizada posterior **102 pruebas,0 fallos/errores/omitidas**.
- Test adicional de backup tuvo fallo de compilación por import estático assertFalse faltante, corregido antes de verificación final.
- `mvn -Dauth.pg.enabled=true -Dtest=PasswordResetPostgresTest test`: primer arranque rechazado por guard antes de datasource al faltar las tres APP_DATABASE explícitas. Se agregaron las mismas credenciales/URL del contenedor aislado a ese contrato; reintento **1 prueba,0 fallos/errores/omitidas, BUILD SUCCESS**. FlywayV1–V8 y validate; cooldown compartido y doble consumo con solo un éxito. No se utilizó15433.
- `mvn verify` final: **390 pruebas contabilizadas,0 fallos,0 errores,4 omitidas, BUILD SUCCESS**. Las4 omitidas son PaymentsPostgresTest(3, laboratorio previo no activado) y PasswordResetPostgresTest(1, ejecutado con éxito por separado). No afirmar390 ejecutadas:386 pasan dentro de verify,4 omitidas.
- `git diff --check` BE/FE código0; escaneo de patrones de credenciales sin coincidencias en archivos candidatos. No se cargaron valores .env en esta feature.

Logs locales: /private/tmp/be008-focused.log, be008-regression.log, be008-postgres.log, be008-verify-final.log y fe019-build-final.log; nombres completos con prefijo /private/tmp/. No se publican logs ni artefactos con sesiones/credenciales. Revisión independiente pendiente de autorización/respuesta; no confundir verificación del autor con revisión independiente.

Configuración posterior y limitación RenderFree SMTP: [smtp-activation.md](smtp-activation.md). Fuente oficial consultada2026-10-28; no se activó transporte ni se cambió plan. Contenedor de test exclusivo `auth008-pg-d739512242`, PostgreSQL16, tmpfs, puerto15434; eliminado después de la prueba junto con sus archivos privados de credenciales, verificando nombre y etiqueta BE008. Laboratorio15433 preservado. Los timeouts/puerto587 están implementados; elegir proveedor no basta si el hosting bloquea ese puerto.

## Revisión independiente autorizada — 2026-10-28

Usuario autoriza segundo agente de solo lectura para código/tests/evidencia BE008/FE019. Agente auth_review iniciado; sin ejecución de tests/aplicación/build, lectura de secretos/data/bases ni operaciones remotas. Hallazgos y veredicto pendientes; el principal corregirá dentro del alcance aprobado y verificará lo afectado.

### Hallazgos iniciales y correcciones de revisión

Revisor auth_review: veredicto inicial REQUIERE CAMBIOS. P1: restore A→B sin usuario→A podía repetir sessionVersion; reutilizar ID tras restore vacío y registrar una cuenta con versión0 también podía revivir JWT de otra identidad. Corregido: generación de sesión impredecible positiva (SecureRandom,62bits con margen para incrementos) al crear y restaurar usuarios, descartando los valores actual/restaurado conocidos. Migrados existentes conservan0 hasta nuevo evento de credenciales/restore; no se editó migración aplicada. Dos regresiones HTTP cubren A→B→A y nuevo registro mismo email/ID después de restore vacío. P2:401 atrasada de tokenA o request anónima tras loginB podía borrarB. Corregido comparando Authorization de request fallida con bearer actual; prueba preservaB y comprueba que su propia401 sí cierre sesión. Configuración SMTP añade verificación explícita de identidad del servidor TLS.

Regresión BE focalizada tras correcciones:22 pruebas,0 fallos/errores/omitidas, BUILD SUCCESS. FE sesión:6 pruebas/6 pasan, TypeScript código0; build posterior código0. No se ejecutó navegador. Re-revisión estática y repetición final de verificación en curso. Un primer intento focalizado de backup tras P1 falló compilación por import assertNotEquals omitido, corregido antes de las22 pruebas exitosas.

### Cierre de revisión y verificación — 2026-10-28

Re-revisión independiente de auth_review: **APPROVED** para la snapshot corregida, sin bloqueantes concretos restantes, condicionada a verificación posterior del principal. El revisor solo leyó código/tests/evidencia, sin tests/build/aplicación ni secretos/datos/remotos. Condiciones satisfechas con ejecuciones posteriores reales:

- Backend `mvn verify`: **392 contabilizadas;388 aprobadas,0 fallos,0 errores,4 omitidas; BUILD SUCCESS**, código0. Cuatro optativas: PaymentsPostgresTest3 (no activadas, sin tocar15433) y PasswordResetPostgresTest1 (pasada por separado).
- `mvn -Dauth.pg.enabled=true -Dtest=PasswordResetPostgresTest test` tras nueva generación de sesión: **1 prueba,0 fallos/errores/omitidas, BUILD SUCCESS**, código0; contenedor nuevo auth008-pg-2dbf48edce en tmpfs15434. Eliminado al terminar con verificación de nombre/etiquetaBE008, junto con sus credenciales temporales. Ninguna base persistente tocada.
- Frontend `node --test tests/session-safety.test.cjs`: **6/6 aprobadas**; TypeScript código0 y `NEXT_PUBLIC_API_URL=https://vortise-be.onrender.com npm run build` posterior código0. Sin navegador por instrucción del usuario.
- `git diff --check` BE/FE código0. Código/tipos/contrato revisados en conjunto; sin commits/push/deploy, correo real ni cambios de contraseña del superadmin real. H2 preexistente permanece excluida.

Resultados anteriores390/5tests quedan como historia y son sustituidos para la snapshot final por392/6tests. Evidencia posterior: /private/tmp/be008-review-focused.log, be008-review-postgres.log, be008-review-verify.log y fe019-review-build.log. Una compilación focalizada previa falló por import de assertNotEquals faltante, corregido antes de las pruebas exitosas.

**Veredicto: implementación local completa, verificada y revisada independientemente.** Pendientes de activación expresamente diferidos: proveedor/transporte de correo compatible, configuración y entrega real, publicación/despliegue y migración remotos bajo autorización específica. No se presenta la verificación local como feature ya disponible en staging. La rotación del superadmin persistido no se ejecutó como parte de esta implementación. Limitaciones documentadas: cola SMTP en memoria, proxy/IP por configurar, JWTbearer en localStorage y ausencia de validación visual por decisión del usuario.
