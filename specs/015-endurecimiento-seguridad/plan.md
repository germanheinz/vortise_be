# Plan 015 — Endurecimiento de seguridad

Spec: [spec.md](./spec.md). Estado: borrador para aprobación. Se ejecuta junto con la [spec 014](../014-roles-matriz-accesos/plan.md).

## Prioridad
| Fase | Objetivo | Hallazgos | Cuándo |
|---|---|---|---|
| **S0 — Contención inmediata** | Cerrar lo que permite alterar datos o entrar con facilidad | S-01 (F0 de la spec 014), S-02 (contención), S-03, S-06 | Antes de cualquier uso real |
| **S1 — Autorización completa** | Roles por endpoint, aislamiento y superficie mínima | S-01 completo, S-07, S-08, S-10 (auditoría base) | Antes de producción |
| **S2 — Sesión y navegador** | Token fuera de JavaScript y cabeceras | S-04, S-05, S-13, S-14 | Antes de producción (o aceptación explícita para demo) |
| **S3 — Cadena de suministro y cuentas** | CI de seguridad, uploads, MFA | S-09, S-11, S-12 | Antes de producción o primeras semanas |
| **S4 — Verificación independiente** | Revisión y pruebas dinámicas | Todos | Cierre |

## S0 — Contención inmediata
1. **F0 de la spec 014:** filtro de escritura para `CONSULTA`/`RESPONSABLE`.
2. **S-02:** sin cambios de modelo por ahora (riesgo aceptado, ver spec §5). La escritura de `rendimientos` y `gastos-fijos` pasa a `ADMINISTRADOR` con los guards de la spec 014; se documenta el riesgo residual.
3. **S-03 límite de intentos:** por cuenta y por IP (hasheada, como en `PasswordResetTransactions`), con ventana y umbral configurables, respuesta uniforme y registro del evento. Almacenamiento en base (como `PasswordResetQuota`) para funcionar con varias instancias.
4. **S-06:** no se eliminan las semillas de demostración (las pruebas las referencian); se restringe su activación a un perfil explícito (`demo`/`local`) para que ningún despliegue sin perfil cree credenciales demo. Se mantienen las pruebas existentes y se agrega una que verifica que, sin ese perfil, no se crea ningún usuario. En producción solo se siembran el catálogo de planes y el `SUPER_ADMIN` desde variables de entorno. Revisar el impacto en el desarrollo local (H2 de archivo con `ddl-auto: update` en el `application.yml` base).

## S1 — Autorización completa
1. Spec 014 F1–F3 (token con rol/obras, guards, reglas finas, gestión de usuarios).
2. **S-02:** diferido; revisar el riesgo aceptado antes de abrir a más de una empresa real.
3. **S-07:** eliminar controladores, casos de uso, repositorios y pruebas de los endpoints sin uso del frontend (inventario previo: confirmar que ninguna otra pieza los usa; sin borrar datos ni tablas, que quedan para una migración aparte con aprobación).
4. **S-08:** inventario de rutas fuera del interceptor; en rutas anidadas verificar que cada ID hijo pertenece a la obra; pruebas de IDOR por ruta.
5. **S-10 auditoría base:** log estructurado de 401/403, cambio de rol/obras, alta/baja de usuarios, cierre de certificados y emisión de reportes (usuario, empresa, obra, acción, resultado; sin datos sensibles).

## S2 — Sesión y navegador
1. Cookie `HttpOnly; Secure; SameSite` con access token ≤ 15 min y refresh token rotativo (hash en base, revocable, detección de reutilización).
2. Backend y frontend en el mismo origen (proxy/`rewrites` de Next.js) para evitar `SameSite=None`.
3. Protección CSRF para métodos de escritura (`Origin`/`Referer` + token si hace falta).
4. Frontend: dejar de leer el token; rol y obras desde `/api/auth/me`; limpiar `localStorage`.
5. Cabeceras en `next.config.mjs`: CSP estricta (con nonce), `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, `frame-ancestors`.
6. Acotar `allowedHeaders` de CORS; evaluar `kid` y rotación de secreto.

## S3 — Cadena de suministro y cuentas
1. **Uploads:** validar tipo real por contenido (magic bytes), re-codificar imágenes, límites por endpoint (bajar el global de 50 MB), nombres generados por el servidor.
2. **CI:** análisis de dependencias (backend y `npm audit`), escaneo de secretos, actualizaciones automáticas.
3. **MFA (TOTP)** para `ADMINISTRADOR` y `SUPER_ADMIN`, con códigos de recuperación.
4. Revisar la validación de la firma del webhook de Stripe.
5. Actualizar `gestion_be/AGENTS.md`.

## S4 — Verificación independiente
Revisión con el agente de seguridad más pruebas dinámicas sobre un entorno efímero: autenticación, roles × endpoints, IDOR, fuerza bruta, uploads, CSRF/XSS. Cierre con veredicto y riesgos aceptados.

## Verificación (según cada AGENTS.md)
- **gestion_be:** pruebas enfocadas con mocks; pruebas HTTP con Spring + H2 efímero previa aprobación de ejecución; un `mvn verify` al cierre.
- **gestion_fe:** `./node_modules/.bin/tsc --noEmit --incremental false` y `npm run build`.
- Informar comandos, resultados y omisiones por repositorio.

## Orden y dependencias
S0 → S1 → S2 → S3 → S4. S0 se despliega por partes sin esperar. S2 depende de que el contrato de `/me` de la spec 014 esté cerrado.
