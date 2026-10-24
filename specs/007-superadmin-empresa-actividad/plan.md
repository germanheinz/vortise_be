# 007 — Plan técnico de backend

## Estado

APROBADO — 2026-10-23. El dueño autorizó la implementación del alcance descrito. Esta aprobación no autoriza ejecución de pruebas/build, acceso a DB persistente, Render ni despliegue.

## Repositorio y dependencias

- Repositorio: `gestion_be`.
- Contrato compartido con `gestion_fe/specs/014-superadmin-empresa-actividad`.
- Requiere agregar una columna nullable con una nueva migración Flyway; no tocar V1/V2 ni usar Hibernate `update` en staging.

## Decisiones técnicas propuestas

1. Añadir `ultimoLoginExitosoEn` de tipo `Instant` nullable a `UsuarioEmpresa`, con nombre de columna explícito snake_case para no depender de la estrategia física implícita.
2. En `AutenticacionUseCase.login`, solo después de validar usuario activo y password correcto, actualizar y persistir `ultimoLoginExitosoEn` antes de construir la sesión. El método debe ser transaccional de escritura; intentos fallidos no alcanzan la escritura.
3. Añadir una migración nueva (propuesta V3) que agregue la columna nullable a `usuarios_empresa`; sin default/backfill. Flyway existente conserva la autoridad del schema para PostgreSQL.
4. Extender `SuperAdminDtos.UsuarioDto` y el query paginado de usuarios con el instante de último login exitoso.
5. Extender `EmpresaDto` con creador y conteos; resolver `creadoPorUsuarioId` mediante batch/proyección, tolerando null o referencia inválida. Evitar consulta individual por empresa.
6. Agregar consultas agregadas/proyecciones para usuarios, proyectos, restricciones y registros de horas por empresa. Contar restricciones/horas a través de proyectos relacionados por `empresaRelacionada`; no inferir por el campo textual de empresa. Evitar joins entre múltiples colecciones que multipliquen conteos y evitar N+1 en la página.
7. Mantener endpoint, filtros, orden, paginación y autorización actuales. No devolver contenido de recursos ni entidad JPA.

## Pruebas propuestas

- Unit tests de autenticación: login correcto cambia timestamp y devuelve sesión; credencial errónea/usuario inactivo no persiste cambios.
- Pruebas de mapping/agregaciones y caso creador ausente/referencia no resoluble.
- Pruebas HTTP/consulta de autorización y conteos con aislamiento validado; el test nunca debe utilizar H2 persistente.
- Aplicar V3 y validar Hibernate contra PostgreSQL desechable local; reiniciar y verificar checksum/versión Flyway. No conectar Render.

## Riesgos y controles

- Usuarios históricos tendrán `null`; no estimar ni migrar datos de actividad desde logs no verificados.
- `creadoPorUsuarioId` puede faltar o no resolver; mostrar null sin excluir la empresa.
- Asociaciones de proyecto legadas pueden faltar; contar solo relaciones existentes, no nombres.
- Actualizar la fecha por cada login genera una escritura adicional por autenticación; no se persigue actividad por request.
- Un test nuevo que arranque Spring debe usar base desechable revisada y aprobación específica de ejecución según `gestion_be/AGENTS.md`.
- Compatibilidad real con Render, TLS, migración remota y release se tratan como aprobaciones separadas.

## Criterio de salida del plan

Aprobación explícita de este plan y del contrato aditivo antes de implementación. Autorización de comandos/ambiente para tests y PostgreSQL local desechable debe documentarse separadamente; no se infiere autorización de despliegue.
