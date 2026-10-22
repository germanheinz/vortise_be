# 004 — Plan técnico

Estado: aprobado para implementación según el alcance de [spec.md](spec.md).

## Diseño backend

- `infrastructure.backup.H2LogicalBackupService`: inspecciona entidades del metamodelo JPA,
  verifica tablas/columnas en JDBC metadata, exporta filas JDBC versionadas a JSON y
  valida/importa snapshots con `JdbcTemplate` dentro de transacciones Spring.
- Construye un grafo FK solo entre tablas JPA allowlisted. Orden topológico para inserts;
  orden inverso para deletes; ciclos y referencias a tablas no mapeadas se rechazan antes
  de mutar.
- Compara huella y estructura antes de borrar. Crea SQL solo con identificadores de
  metadata allowlisted escapados por el driver; datos siempre con placeholders.
- Reinicia identidades H2 con `ALTER TABLE ... ALTER COLUMN ... RESTART WITH` al final,
  usando `MAX(id)+1` o 1 para tablas vacías. Esta operación DDL puede confirmar la
  transacción: una falla durante esa fase no tiene garantía de rollback.
- `AdminBackupController`: endpoints GET y multipart POST, ambos protegidos por la
  autoridad exacta `ROLE_SUPER_ADMIN`; contenido limitado a 50 MiB con límite de request
  multipart de 52 MiB. La exportación rechaza snapshots que excedan el límite aceptado por
  restore.
- `DatabaseInitializationPolicy`: captura una vez al completar la inicialización del
  contexto si alguna tabla de usuario ya contiene filas. `DataSeeder`,
  `UsuarioDemoSeeder`, `SuperAdminSeeder` y `PlanSuscripcionSeeder` consultan la misma
  política antes de actuar; el estado fresh permite que todos los runners seed iniciales.

## Verificación planeada (no ejecutar en esta tarea)

- Tests unitarios para política de startup y tests de integración con H2 `mem:` aislado
  para round-trip/temporal/nulls, endpoints y autorización, archivo inválido/huella,
  rollback SQL, orden FK e identity post-restore.
- Comandos sugeridos para el usuario tras revisar aislamiento: `mvn
  -Dtest=H2LogicalBackupHttpTest,DatabaseInitializationPolicyTest,DataSeederTest,UsuarioDemoSeederTest,SuperAdminSeederTest,PlanSuscripcionSeederTest
  test`, luego `mvn verify` si autoriza el alcance completo.
- Ningún test, Maven, base/API real, install, deploy, git ni comando de datos forma parte
  de esta implementación.
