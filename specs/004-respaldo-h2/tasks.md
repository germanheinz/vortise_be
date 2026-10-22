# 004 — Tasks

- [x] Descubrimiento: revisar AGENTS, seguridad, mapeos JPA, seeders, configuración H2 y
  patrones de test.
- [x] Especificación/plan aprobado por el usuario; alcance incluye backup completo y
  restore lógico, sin restart, backup de datos previo ni ejecución contra DB existente.
- [x] Implementar export/import JDBC versionado, allowlist JPA, validación total previa,
  orden FK, rollback transaccional y ajuste identity H2.
- [x] Agregar endpoints exactos superadmin y manejo de tamaño/archivo. Backend multipart y
  servicio alineados a 50 MiB; requests multipart limitados a 52 MiB y exportaciones
  superiores a 50 MiB rechazadas.
- [x] Aplicar política compartida de DB inicializada a los cuatro seeders, sin afectar
  `reset-demo` explícito.
- [x] Agregar tests de integración para round-trip, auth, validaciones, rollback, orden FK
  e identity; agregar tests unitarios aislados para el snapshot de la política, omisión de
  los cuatro seeders y preservación del reset demo explícito.
- [x] Revisar diagnósticos estáticos de archivos editados. Corregidos los tres diagnósticos
  reportados en `H2LogicalBackupHttpTest`; siguen avisos preexistentes en `DataSeeder` y
  `application.yml`, fuera del alcance solicitado.
- [x] Revisión independiente estática: no se encontraron defectos accionables de contrato,
  autorización, orden FK, rollback de filas, identity o persistencia de seeders. Esta
  revisión no reemplaza la ejecución de pruebas.
- [ ] Verificación Maven/tests: pendiente del usuario; no se ejecutaron Maven, tests, build,
  API, comandos de datos ni operaciones git.
- [x] Cierre de implementación: conservar como limitación que el DDL H2 de reinicio de
  identity puede confirmar la transacción e impedir rollback en esa fase.
