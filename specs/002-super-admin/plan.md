# Plan — Superadministrador en H2 poblada

## Diseño aprobado

- Mantener `DatabaseInitializationPolicy` para los seeders demo/globales y no cambiar la
  política general de inicialización.
- Hacer que `SuperAdminSeeder` continúe al procesamiento de su cuenta configurada cuando
  `SeedRunPolicy` indica que la base ya está inicializada. El resto de los seeders seguirá
  omitiendo la DB poblada.
- Usar exclusivamente las variables `APP_SUPERADMIN_EMAIL` y
  `APP_SUPERADMIN_PASSWORD`; sin defaults codificados. Mantener BCrypt y el mínimo actual
  de 12 caracteres.
- Preservar la protección contra colisión de email: no promover cuentas existentes ni
  cambiar su contraseña.
- Cubrir el comportamiento en tests unitarios que no conectan con base persistente y en
  el test existente sobre seeders que omiten una DB inicializada.

## Archivos

- `src/main/java/com/vortise/gestion/infrastructure/config/SuperAdminSeeder.java`
- `src/test/java/com/vortise/gestion/infrastructure/config/SuperAdminSeederTest.java`
- `src/test/java/com/vortise/gestion/infrastructure/config/SeedersSkipOnInitializedDatabaseTest.java`
- Esta especificación y `tasks.md`.

No iniciar aplicaciones, ejecutar seeders, leer/escribir credenciales, modificar la base,
ni ejecutar tests o Maven dentro de esta tarea.