# 007 — Estado y evidencia backend

**Estado:** implementación fuente completada dentro del alcance aprobado; verificación de runtime pendiente. Última actualización: 2026-10-23.

## Aprobaciones y límites

- [x] Spec/plan y contrato aprobados para implementación por el dueño.
- [x] Implementación fuente y cambios de pruebas autorizados.
- [ ] Ejecución de pruebas/build y validación Flyway/PostgreSQL: requieren autorización separada conforme a AGENTS.md.
- [ ] Revisión independiente de QA/seguridad y cierre.
- [x] No consultar ni modificar DB persistente, Render o recursos remotos; no desplegar.

## Tareas

- [x] Añadir `Instant` nullable de último login exitoso y migración Flyway V3 aditiva, sin editar V1/V2.
- [x] Persistir el instante solo después de validar credenciales y estado activo.
- [x] Extender DTOs y consultas superadmin con timestamp, creador y cinco conteos por empresa.
- [x] Añadir fuente de pruebas unitarias para login fallido/inactivo/exitoso y mapping de métricas.
- [ ] Ejecutar pruebas, validar V3/Hibernate y restart de Flyway en PostgreSQL desechable.
- [ ] Revisión independiente de QA/seguridad y cierre.

## Evidencia de implementación

- `UsuarioEmpresa` almacena `ultimoLoginExitosoEn`; V3 agrega `ultimo_login_exitoso_en` nullable sin default/backfill. V1 y V2 permanecen intactas.
- `AutenticacionUseCase.login` guarda `Instant.now()` solo después de validar usuario activo y contraseña correcta; fallos no llegan a la escritura.
- `GET /api/admin/usuarios` incluye el instante nullable en su DTO paginado.
- Listado de empresas usa consultas agrupadas/proyecciones por página para usuarios totales/activos, proyectos enlazados por `empresaRelacionada`, restricciones, registros de horas, creador y suscripción; no carga colecciones ni ejecuta lookup por fila. Todas las métricas omiten superadmins y la empresa plataforma sigue excluida por el filtro existente.
- Pruebas fuente añadidas/actualizadas: `AutenticacionUseCaseTest` y `SuperAdminConsultaUseCaseTest`.
- No se ejecutaron tests, Maven, migraciones ni consultas de base de datos. La validez JPQL, serialización ISO-8601 y compatibilidad del esquema requieren verificación autorizada.
