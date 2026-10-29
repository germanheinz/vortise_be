# 009 — Plan técnico backend

1. Añadir RestriccionObra.area nullable, longitud 120; extender DTO entrada/salida y RestriccionMapper. Mantener constructor previo de RestriccionCreateDto para consumidores fuente existentes.
2. Nueva V12__restriction_area.sql y ALTER idempotente para H2 local en schema.sql. No aplicar ni reiniciar servicios desde esta tarea. Respaldos previos al nuevo esquema requieren revisión separada de compatibilidad; no modificar el servicio de backup.
3. Reutilizar patrón RubroPlanning mediante RestriccionPlanning para normalizar área y validar cronología; invocarlo desde creación y actualización antes de alterar estado persistente.
4. Ampliar RestriccionUpdateDto con área/responsable/fechas y flag actualizarFechas. Mantener constructor previo de tres argumentos. UpdateRestriccionUseCase conserva los campos omitidos y usa @Transactional para guardar y mapear sin depender de Open Session in View.
5. No añadir endpoint agregado: las consultas existentes entregan todas las restricciones autorizadas y el frontend agrupa sin requests adicionales. No devolver datos de empresas no autorizadas ni inferir área desde entidades ajenas.
6. Usar catálogo genérico AREA_RESTRICCION ya soportado por configuración empresarial; el campo de restricción es un snapshot de nombre, no una nueva FK ni un permiso.

## Verificación propuesta

- Fuente de pruebas context-free: CreateRestriccionUseCaseTest y UpdateRestriccionUseCaseTest para mapping, normalización, cronología, limpieza explícita y preservación de patches antiguos.
- Fuente HTTP añadida a RestriccionDetalleHttpTest: editar/leer en empresa propia, denegar otra empresa y anónimo. La suite inicia Spring/H2 y requiere aprobación del aislamiento de [testing-checklist.md](../../docs/testing-checklist.md).
- Comandos pendientes, no ejecutados: `mvn -Dtest=CreateRestriccionUseCaseTest,UpdateRestriccionUseCaseTest test`; luego HTTP y `mvn verify` bajo aprobación específica. Probar V12 y restart en PostgreSQL desechable revisado, nunca la base persistente del workspace.
- Revisión independiente de seguridad/QA y cierre posteriores a la ejecución autorizada.