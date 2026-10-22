# 004 — Respaldo lógico y restauración H2

## Estado

Aprobado explícitamente para implementación backend (2026-10-21). El alcance aprobado
incluye respaldo lógico completo y reemplazo transaccional de datos en H2 abierto; no
incluye ejecución de restauración sobre una base real, restart del servicio ni backup
previo de datos completos. Ninguna base existente debe modificarse durante este trabajo.

## Contrato

- `GET /api/admin/backup`: solo `ROLE_SUPER_ADMIN`; responde JSON versionado como
  `application/json` y con `Content-Disposition` indicando el nombre del archivo.
- `POST /api/admin/backup/restore`: solo `ROLE_SUPER_ADMIN`; multipart con campo `file`;
  respuesta JSON con `message` y `summary`.
- No se crea endpoint independiente de reset.
- No se incluyen fotos almacenadas en filesystem, archivos `.env`, secretos ni contenido
  fuera de las tablas JPA. Se incluyen todas las filas de las entidades mapeadas,
  incluidas credenciales/password hashes, empresas/usuarios, catálogos, proyectos y
  descendientes, suscripciones, pagos y filas de Stripe.

## Requisitos funcionales y de seguridad

- Exportar mediante JDBC únicamente tablas obtenidas de las entidades del metamodelo JPA;
  verificar cada tabla/columna contra metadatos JDBC y excluir esquemas/tablas del motor.
- El formato contiene versión, huella de esquema, tablas, columnas/tipos JDBC y filas con
  valores nulos/textuales/numéricos/binarios/temporales representables sin recorrer
  relaciones Java. Preservar IDs y valores de columnas.
- Restaurar solo si la versión, tamaño, huella, conjunto exacto de tablas/columnas/tipos,
  estructura de filas, nullability y valores tipados son válidos. Rechazar SQL o tablas
  desconocidas; identificadores solo desde metadatos allowlisted y con quote; todos los
  valores viajan como parámetros JDBC.
- Resolver el orden de claves foráneas antes de cualquier mutación; borrar en orden
  inverso e insertar en orden forward. Rechazar ciclos no resolubles antes de borrar.
- Ajustar columnas identity a `MAX(id)+1` después de insertar IDs explícitos.
- Ejecutar el reemplazo en transacción Spring/JDBC. Validaciones ocurren antes del primer
  delete. Errores de filas durante delete/insert revierten la transacción en H2; el DDL
  de reinicio de identity de H2 puede confirmar la transacción y no es rollbackable. Por
  eso los reinicios se realizan al final y sus riesgos/limitaciones deben quedar claros.
- Los seeders de arranque usan una única decisión tomada antes de ejecutar runners: seed
  solo si la base está vacía al inicio. Una restauración en una base existente no se
  completa ni repara con seeds en el siguiente arranque. Se conserva el reset demo
  explícito existente.
- El respaldo contiene datos sensibles, incluidos hashes de contraseñas, y debe guardarse
  cifrado o en un lugar con controles de acceso equivalentes a la base.

## Aceptación

1. Rutas y autorización server-side exacta `@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")`.
2. Round-trip lógico conserva tablas, datos, nulls, IDs y valores temporales.
3. Archivos inválidos/incompatibles, datos mal formados y ciclos se rechazan sin borrar.
4. Un fallo SQL de inserción revierte los deletes/inserts cuando H2 lo soporta.
5. Identidades siguen generando IDs sin colisión luego de restore.
6. En DB ya poblada/restaurada, los cuatro seeders no crean/reparan filas ni llaman Stripe.
7. Tests añadidos para export/import, autorización, validación, orden FK, rollback e
   identity. Los tests no se ejecutan durante esta implementación.

## Riesgos y verificaciones pendientes

- Los comandos de DDL de H2 para reiniciar identity son una frontera de commit; un fallo
  durante esa etapa puede dejar los datos importados confirmados aunque la transacción ya
  no pueda revertirlos. Verificar en la versión H2 resuelta y conservar esta limitación.
- Backup y restore admiten hasta 50 MiB de contenido; multipart admite requests de hasta
  52 MiB para dejar margen a los encabezados. La exportación también se rechaza si supera
  50 MiB, para no ofrecer un archivo que el endpoint de restore no pueda aceptar. El JSON
  se construye en memoria, por lo que respaldos cercanos al límite requieren memoria
  adicional durante la serialización.
- La huella compara estructura de columnas y relaciones FK de las tablas mapeadas; no
  pretende transportar DDL, índices, triggers, usuarios del motor ni archivos externos.
- Build/tests y revisión independiente quedan pendientes para ejecución del usuario.
