# Plan técnico

## Calendario laboral

1. Agregar columnas de calendario a empresas mediante una migración nueva. Copiar los valores y los identificadores existentes por empresa. Representar explícitamente la ausencia de calendario y comprobar la paridad fila por fila.
2. Cambiar las lecturas y escrituras de Spring a un objeto de valor integrado en Empresa. Cubrir inicialización, edición, baja, alta de obra y previsto vs. real. Verificar la asignación de IDs para nuevos calendarios si el contrato actual los expone.
3. Coordinar el corte de escritura y la reversión para evitar divergencias entre versiones. Resolver antes del corte la clave foránea de la tabla antigua, que puede impedir la baja de una empresa si permanecen filas históricas.
4. Comprobar restauración de respaldos y definir el retiro físico de la tabla en una fase posterior con aprobación explícita.

Resultado esperado del corte: una entidad JPA y los repositorios específicos del calendario salen del modelo activo. La tabla histórica sigue contando en el esquema hasta su retiro físico.

## Consultas y registros de obra

Tomar una referencia de cantidad de consultas, filas devueltas y tiempos con datos representativos en un entorno aislado. Revisar planes de ejecución para filtros por empresa y obra, y por fecha donde ya exista ese filtro. Agregar índices solo cuando el plan lo justifique. Introducir paginación compatible con el frontend y verificar que los indicadores agreguen el conjunto completo que corresponda.

El calendario de horas registra intervalos. El asistente de tareas guarda avance por actividad. Comparar registros equivalentes y consumidores por campo antes de diseñar una fuente única.

## Condiciones del corte

- V31 copia todos los calendarios existentes y conserva sus IDs. Los nuevos usan el ID de empresa con signo negativo como identificador opaco y estable. El frontend no usa ese valor para construir rutas.
- Detener los escritores de la versión anterior antes de aplicar V31 y habilitar la versión nueva solo después de comprobar el backfill. La tabla histórica no recibe cambios de configuración desde la versión nueva. Si hay que volver atrás después de aceptar nuevas escrituras, reconciliar primero los datos desde empresas hacia la tabla anterior en una copia validada.
- La FK histórica pasa a eliminar por cascada la fila antigua cuando se elimina la empresa, igual que hacía el servicio anterior. V31 no elimina filas durante el corte.
- H2 persistente del perfil base tiene Flyway deshabilitado. Su actualización de Hibernate no hace el backfill ni cambia la FK histórica. Las bases H2 existentes requieren un procedimiento de migración custodio antes de ejecutar esta versión. El respaldo H2 incluye las filas históricas si la tabla existe; la versión anterior del formato contiene una huella distinta tras el cambio de columnas y requiere una prueba de restauración antes del corte.
