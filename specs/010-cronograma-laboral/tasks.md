# 010 — Estado y evidencia

## Implementación aprobada 2026-11-01

- [x] Persistir y exponer el plazo nullable de la obra; PUT antiguo lo conserva y el nuevo formulario puede limpiarlo.
- [x] Preparar migración Flyway V15 y ajuste local H2, sin aplicarlos.
- [x] Bloquear en el caso de uso común la creación después de la fecha límite inclusiva usando fecha Argentina; no limitar edición.
- [x] Ordenar la respuesta detallada de rubros por código numérico.
- [x] Agregar pruebas fuente para deadline, validación de creación, mapeo/orden y migración.
- [x] Diagnósticos editoriales de los archivos backend cambiados sin errores nuevos.
- [ ] Maven/tests, aplicación de V15, acceso a bases, deploy y revisión independiente pendientes de autorización.
