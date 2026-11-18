# Plan 014 — Roles con Spring Security y permisos en el token

Spec: [spec.md](./spec.md). Estado: borrador para aprobación.

## 1. Decisiones técnicas
| Tema | Decisión |
|---|---|
| Roles | `ADMINISTRADOR`, `JEFE_DE_OBRA`, `CONSULTA` (+ `RESPONSABLE` tratado como consulta; `COLABORADOR` sin cambios). Sin enums nuevos |
| Permisos en el token | Claims `rol` y `obras` (solo jefe); las authorities salen del token |
| Revocación | Se mantiene la validación contra la base de `activo` y `sessionVersion`; `sessionVersion++` al cambiar rol/obras/estado |
| Guards | Constantes `SOLO_GERENTE` y `GERENTE_O_JEFE` en una clase única (p. ej. `infrastructure/security/Guards.java`) usadas en `@PreAuthorize` |
| Obra asignada | Una sola comprobación en `TenantAccessInterceptor` (rutas por proyecto y recursos hijos) usando el claim `obras` |
| Default-deny | Filtro de solo lectura (F0), conservado como defensa en profundidad |
| Datos | Tabla `usuario_obra` (V22, aditiva) |
| Frontend | Helper por rol (`esGerente`, `puedeEscribir`, `puedeEnObra`) en `lib/permissions.ts`; sin librería nueva |

## 2. Fases

### F0 — Hotfix de solo lectura (independiente, sin migración)
- `SoloLecturaWriteGuard` (filtro/interceptor) en `infrastructure/security/`: 403 `ACCESO_SOLO_LECTURA` para `CONSULTA`/`RESPONSABLE` (no `SUPER_ADMIN`) en `POST/PUT/PATCH/DELETE` de `/api/**`.
- Excepciones cerradas: `/api/auth/**`, restablecer contraseña, aceptar invitación, `/api/stripe/webhook`, `/api/me/foto`.
- Propiedad de configuración para desactivarlo; inventario previo de `POST` que sean lecturas.
- Prueba parametrizada sobre todos los mappings de escritura × {CONSULTA, RESPONSABLE, ADMINISTRADOR, JEFE_DE_OBRA, COLABORADOR}.

### F1 — Token con rol y obras, y obras asignadas
- `V22__usuario_obra.sql` (tabla nueva, PK compuesta, FKs, índice por `proyecto_id`); relación en `UsuarioEmpresa` (`@ElementCollection` como `empresasAsignadas`).
- `JwtService.generate(...)`: añade `obras`. Login y `/api/auth/me` devuelven `rol` y `obras`.
- `JwtAuthenticationFilter`: authorities desde los claims del token; conserva la validación de base.
- `TenantAccessInterceptor`: regla de obra asignada para `JEFE_DE_OBRA` en escrituras (403 `OBRA_NO_ASIGNADA`).
- `scripts/extract-empresas.py`: clasificar `usuario_obra`.
- Corregir `gestion_be/AGENTS.md` (Spring Boot 4.x, Flyway presente).

### F2 — Guards por endpoint
Aplicar `SOLO_GERENTE` / `GERENTE_O_JEFE` según el anexo A. Orden por riesgo: tareas, restricciones, reportes → certificados (reemplaza `ESCRITURA`) → plantas, rendimiento, presupuesto, finanzas → proyecto/empresa/configuración/importación.
- Reglas finas en los casos de uso: restricción una sola vez para jefe, ítem completado con certificado abierto, tarea abierta vs cerrada (403 `REGLA_DE_ESTADO`).
- Test que enumera los endpoints de escritura y exige guard o excepción declarada.

### F3 — Gestión de usuarios y obras
- `UsuarioEmpresaController`/`UsuarioEmpresaUseCase`: solo `ADMINISTRADOR`; rol y obras asignables; reglas anti-escalada; `sessionVersion++`.
- DTO: `rol`, `obrasAsignadas`.

### F4 — Frontend
- `types/index.ts`, `lib/session.ts`, `lib/permissions.ts`, `lib/use-session-capabilities.ts`, `lib/http-client.ts` (403 por `code`).
- `components/layout/*`, `components/certificados/CertificadosObra.tsx` y pantallas de alta/edición: ocultar/deshabilitar por rol.
- `components/empresas/UsuariosEmpresaSection.tsx`: rol y selector de obras para jefe.

### F5 — Verificación y cierre
- Tabla de pruebas endpoint × 3 roles, IDOR por obra, aislamiento entre empresas, token con `sessionVersion` vieja o usuario inactivo.
- Revisión de seguridad independiente y cierre con riesgos aceptados.

## 3. Archivos principales
**gestion_be:** `SoloLecturaWriteGuard`, `Guards`, `V22__usuario_obra.sql`, `JwtService`, `JwtAuthenticationFilter`, `TenantAccessInterceptor`, `UsuarioEmpresa`, `UsuarioEmpresaUseCase/Controller/Dto`, `PerfilUseCase`/`AutenticacionUseCase`, controladores de escritura, `CertificadoObraUseCase`, casos de uso de restricciones/tareas, `scripts/extract-empresas.py`, `AGENTS.md`.
**gestion_fe:** `types/index.ts`, `lib/permissions.ts`, `lib/session.ts`, `lib/use-session-capabilities.ts`, `lib/http-client.ts`, `components/layout/*`, `CertificadosObra.tsx`, `UsuariosEmpresaSection.tsx`.

## 4. Compatibilidad
Login y `/me` solo **añaden** `obras`. Verificar compatibilidad DTO ↔ tipos TS al cerrar F1 y F4. Nuevos códigos 403: `ACCESO_SOLO_LECTURA`, `OBRA_NO_ASIGNADA`, `REGLA_DE_ESTADO`.

## 5. Verificación (según cada AGENTS.md)
- **gestion_be:** pruebas enfocadas sin contexto (`mvn -Dtest=<Clases> test`). Las HTTP arrancan Spring/H2 y requieren aprobación de ejecución (base efímera, nunca el H2 persistente). Cierre: un `mvn verify`.
- **gestion_fe:** `./node_modules/.bin/tsc --noEmit --incremental false` y `npm run build`.
- Reportar comandos, resultados y omisiones por repositorio.

## 6. Orden y rollback
F0 → F1 → F2 → F3 → F4 → F5. F0 se despliega sin esperar al resto.
- F0: propiedad de configuración o revert.
- F1: migración aditiva; el token con `obras` es compatible hacia atrás.
- F2: activar los guards por módulo; un revert del módulo restaura el comportamiento previo, quedando el filtro de F0 como red de seguridad.
