# Tasks 014 — Roles con Spring Security y permisos en el token

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)
Estado global: **pendiente de aprobación de spec y plan**. Nada implementado.

Hitos: H1 especificación · H2 plan aprobado · H3 implementación con checks enfocados · H4 verificación completa · H5 revisión independiente y cierre.

| ID | Fase | Tarea | Depende de | Estado | Criterio de aceptación |
|---|---|---|---|---|---|
| T-001 | H1 | Spec y plan 014 simplificados | — | Hecho | spec.md y plan.md |
| T-002 | H2 | Aprobación de spec/plan y de Q-1 a Q-3 | T-001 | Pendiente | Decisión registrada |
| T-010 | F0 | Inventariar endpoints de escritura y `POST` que sean lecturas | T-002 | Pendiente | Lista de excepciones revisada |
| T-011 | F0 | `SoloLecturaWriteGuard` + propiedad de desactivación | T-010 | Pendiente | 403 `ACCESO_SOLO_LECTURA` a CONSULTA/RESPONSABLE |
| T-012 | F0 | Prueba endpoints × 5 roles | T-011 | Pendiente | ADMIN/JEFE/COLABORADOR sin cambios |
| T-013 | F0 | Aprobar y ejecutar pruebas HTTP (base efímera); desplegar con aprobación | T-012 | Pendiente | Resultados reportados |
| T-020 | F1 | `V22__usuario_obra.sql` + relación en `UsuarioEmpresa` | T-002 | Pendiente | Migración aditiva probada en base efímera |
| T-021 | F1 | `JwtService`: claim `obras`; login y `/me` lo devuelven | T-020 | Pendiente | Token trae `rol` y `obras` |
| T-022 | F1 | `JwtAuthenticationFilter`: authorities desde el token + validación de base | T-021 | Pendiente | Token viejo/usuario inactivo rechazado |
| T-023 | F1 | `TenantAccessInterceptor`: obra asignada para jefe | T-022 | Pendiente | 403 `OBRA_NO_ASIGNADA`, sin IDOR |
| T-024 | F1 | `extract-empresas.py` (clasificar `usuario_obra`) y `AGENTS.md` | T-020 | Pendiente | Prueba de spec 012 verde; documento actualizado |
| T-030 | F2 | Clase `Guards` con `SOLO_GERENTE` y `GERENTE_O_JEFE` | T-022 | Pendiente | Usada en `@PreAuthorize` |
| T-031 | F2 | Guards en tareas, restricciones y reportes | T-030, T-023 | Pendiente | Anexo A cumplido en esos módulos |
| T-032 | F2 | Guards en certificados (D3) | T-030, T-023 | Pendiente | Jefe solo en obra asignada |
| T-033 | F2 | Guards en plantas, rendimiento, presupuesto, finanzas | T-030 | Pendiente | Anexo A cumplido |
| T-034 | F2 | Guards en proyecto, empresa, configuración, importación | T-030 | Pendiente | Dueño conserva facturación |
| T-035 | F2 | Reglas finas (restricción una vez, ítem completado, tarea abierta) | T-031, T-032 | Pendiente | `REGLA_DE_ESTADO` |
| T-036 | F2 | Test que exige guard o excepción en todo endpoint de escritura | T-034 | Pendiente | Falla al agregar endpoint sin guard |
| T-040 | F3 | Gestión de usuarios: rol, obras, anti-escalada, `sessionVersion++` | T-022 | Pendiente | Criterio 7 de la spec |
| T-050 | F4 | Tipos TS, sesión y helper por rol | T-021 | Pendiente | `tsc` sin errores |
| T-051 | F4 | Reemplazar checks de rol sueltos y manejar 403 por `code` | T-050 | Pendiente | Sin acciones visibles rechazables |
| T-052 | F4 | Pantalla de usuarios: rol y obras | T-040, T-050 | Pendiente | Estados de carga/error explícitos |
| T-060 | F5 | Pruebas: endpoint × 3 roles, IDOR, aislamiento entre empresas, token inválido | T-036 | Pendiente | Todas verdes |
| T-061 | F5 | Verificación completa: backend (`mvn verify`) y frontend (`tsc`, `npm run build`) | T-060, T-052 | Pendiente | Resultados reales por repositorio |
| T-062 | H5 | Revisión de seguridad independiente y cierre | T-061 | Pendiente | Hallazgos resueltos o aceptados |

## Aprobaciones de ejecución a solicitar (agrupadas)
1. Pruebas enfocadas sin contexto (mocks).
2. Pruebas HTTP con Spring + H2 efímero, incluida la migración V22 sobre base vacía.
3. `mvn verify`, `tsc` y `npm run build`.
No se autoriza nada sobre el H2 persistente, despliegues ni escrituras remotas sin aprobación explícita adicional.

## Evidencia

### Estado tras la S1 (2026-11-18, gestion_be)
| Tarea | Estado |
|---|---|
| T-020 `V22__usuario_obra.sql` + `UsuarioEmpresa.obrasAsignadas` | Hecho (migración no ejecutada contra una base real) |
| T-021 claim `obras`; login lo devuelve | Hecho (`/api/me` aún no devuelve `obras`: va con F4) |
| T-022 authorities desde el token + validación de base | Hecho |
| T-023 obra asignada en `TenantAccessInterceptor` | Hecho (403 `OBRA_NO_ASIGNADA`) |
| T-024 `extract-empresas.py` | Hecho (17 pruebas python OK); `AGENTS.md` sigue pendiente |
| T-030 a T-034 guards | Hecho, con la clase `Guards` (ver cambios de diseño) |
| T-035 reglas finas | Parcial: restricción una sola vez (hecho), certificado abierto (ya existía), tareas: no existe estado abierta/cerrada, se aplicó "solo las que cargó el jefe" |
| T-036 test de guards | Hecho (`WriteEndpointsGuardTest`) |
| T-040 gestión de usuarios y obras | Hecho (`UsuarioObrasAsignadasTest`, 7 pruebas) |
| T-050 a T-052 frontend, T-060 a T-062 | Pendiente |

### Cambios de diseño respecto de la spec (a confirmar)
- **Guards (`infrastructure/security/Guards`)**: `SOLO_ADMINISTRADOR`, `GERENTE` (`ADMINISTRADOR` + `COLABORADOR`), `OBRA` (`ADMINISTRADOR` + `COLABORADOR` + `JEFE_DE_OBRA`) y `CERTIFICADOS` (`ADMINISTRADOR` + `JEFE_DE_OBRA`, como ya estaba). `COLABORADOR` se conserva donde ya tenía acceso (Q-2, sin cambios).
- **Permisos desde el token**: el rol del token debe coincidir con el de la cuenta; si no, la sesión se rechaza. `obras` del token se ignora para cualquier rol que no sea `JEFE_DE_OBRA`.
- **`JEFE_DE_OBRA` pierde**: editar obra, plantas, importar Excel, eliminar tareas/horas/restricciones, editar la fecha de corte de un certificado, y checkout/portal de suscripción.
- **Restricción**: el jefe solo puede cambiar el estado (con la posición de la tarjeta) y solo si aún no está `RESUELTO` (403 `REGLA_DE_ESTADO`); editar otros datos devuelve 403.
- **Tareas**: no hay estado abierta/cerrada; el jefe solo modifica las que cargó él (`REGLA_DE_ESTADO`). Los cierres por certificado ya bloquean ediciones para todos los roles.
- **Sin cambios de comportamiento**: `EmpresaConfiguracionController` mantiene su lectura limitada a `ADMINISTRADOR`/`COLABORADOR` (el jefe y consulta no ven la configuración de empresa; la matriz dice "Consulta"): decisión pendiente.

### Cierre de F3/F4 y verificación (2026-11-18)
**Cambios:** `/api/me` devuelve `obras` (solo para `JEFE_DE_OBRA`); la lectura de la configuración de empresa queda abierta a todos los roles (las escrituras siguen siendo `ADMINISTRADOR`); `CreateRestriccionUseCase` rechaza crear restricciones fuera de plazo (inicio + `pedidosDeObraDias`, todos los roles, solo en la creación).
**Frontend (`gestion_fe`):** `lib/permissions.ts` y `use-session-capabilities` derivan permisos del rol y de las obras de la sesión; se ocultan o deshabilitan acciones en obra (editar/eliminar), tareas, restricciones, horas, certificados (fecha de corte solo administrador), reportes (imágenes y emisión), cronograma y estructura de actividades; `TeamAccessBoundary` bloquea las pantallas de solo alta/edición para jefe y roles de solo lectura; `UsuariosEmpresaSection` permite asignar obras a un jefe. Es solo ayuda de interfaz: el backend decide.
**Ejecutado:**
- Backend, sin contexto de Spring: `mvn -o -Dtest='!<16 clases @SpringBootTest>' -Dsurefire.failIfNoSpecifiedTests=false test` → 497 pruebas, 0 fallos.
- Backend, 11 clases HTTP con H2 en memoria (autorizadas por el usuario): 132 pruebas, 121 verdes, 11 fallos. **Los mismos 11 fallan sobre una copia limpia de `HEAD`** (preexistentes): 3 casos de entrada mal formada devuelven 500 en lugar de 400 (`PlantaAuthorizationHttpTest` ×2, `Quality14HttpTest` ×1) y 8 de `H2LogicalBackupHttpTest` por "Ciclo de clave foránea no resoluble en RUBROS_OBRA".
- Frontend: `./node_modules/.bin/tsc --noEmit --incremental false` sin errores en el código fuente (solo avisos en carpetas generadas `.next*`); `npm run build` OK en el segundo intento.
**No ejecutado:** la migración V22 contra PostgreSQL real, pruebas Postgres/Testcontainers, `mvn verify` completo, pruebas de interfaz (no hay pruebas de componentes para estos cambios).
**Límites:** `ObraTareaDto` no expone quién registró la tarea, así que el jefe ve "Editar" en cualquier tarea y el backend responde 403 `REGLA_DE_ESTADO` con mensaje si no es suya. `AGENTS.md` del backend sigue pendiente de actualizar.

### Alta de usuarios con rol desde la plataforma (2026-11-18)
**Decisión del usuario:** invitación con enlace de un solo uso; roles invitables `ADMINISTRADOR`, `JEFE_DE_OBRA` y `CONSULTA`; el email queda fijado en la invitación. Solo `ADMINISTRADOR` (gerente) gestiona invitaciones.
**Backend:** migración `V24__invitaciones_usuario.sql` (`invitaciones_usuario`, `invitacion_usuario_obras`); `InvitacionUsuarioUseCase`; `InvitacionUsuarioController` (`/api/empresas/{empresaId}/usuarios/invitaciones`: crear, listar, revocar) y `AceptarInvitacionUsuarioController` (público `/api/auth/invitaciones-usuario/consultar|aceptar`, abre sesión con cookies).
**Reglas:** token de 43 caracteres, solo se guarda su hash, vigencia 72 h, enlace `{app.frontend-url}/invitacion-usuario#token=...` (el token va en el fragmento y se muestra una sola vez); una invitación pendiente por email y empresa; cupo = usuarios activos + invitaciones pendientes ≤ `limiteUsuarios`, revalidado al aceptar; obras solo para jefe y deben pertenecer a la empresa; `RESPONSABLE` y `COLABORADOR` no son invitables; contraseña de 8 a 72; quien acepta no puede elegir rol, email, empresa ni obras. No se envía email: el gerente copia el enlace.
**Frontend:** `lib/api/invitaciones-usuario.ts`, `components/empresas/InvitarUsuario.tsx` (en `UsuariosEmpresaSection`), página pública `app/invitacion-usuario/page.tsx`.
**Ejecutado:** `InvitacionUsuarioUseCaseTest` (13) y `CookieSessionHttpTest` (11, H2 en memoria autorizado); backend sin contexto de Spring 548 pruebas, 0 fallos; frontend `node --test tests/*.test.cjs` 254 pruebas, 0 fallos; `tsc` sin errores en código fuente; `npm run build` OK en el segundo intento.
**No ejecutado:** V24 contra PostgreSQL real, e2e Playwright, pruebas de componentes de la interfaz, envío de email (no existe).
