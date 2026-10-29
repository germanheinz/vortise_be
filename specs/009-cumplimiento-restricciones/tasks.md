# 009 — Estado y evidencia backend

Fecha: 2026-10-29. Implementación fuente completa; verificación ejecutable y cierre pendientes.

- [x] Aprobación del dueño para Área independiente, fórmula de cumplimiento y días corridos.
- [x] Modelo, DTO de entrada/salida, mapper y creación extendidos; validación compartida sin inferencias históricas.
- [x] PATCH ampliado y transaccional, preservando cambios antiguos de prioridad/estado/orden y permitiendo limpieza explícita de fechas.
- [x] V12 y ajuste local de schema.sql preparados, sin ejecutarlos.
- [x] Fuente de pruebas de creación/actualización y caso HTTP de pertenencia añadida.
- [x] Diagnósticos editoriales de modelo/DTO/mapper/planning/creación y pruebas unitarias nuevas sin errores.
- [ ] UpdateRestriccionUseCase conserva advertencia de nulabilidad en findById(restriccionId), en la llamada preexistente.
- [ ] RestriccionDetalleHttpTest contiene una importación preexistente sin resolver: UsuarioDemoSeeder. No corregida fuera de alcance; bloquea la verificación de esa fuente hasta resolver el contexto del repositorio.
- [ ] No se ejecutaron Maven/tests, V12, SQL, servicios, endpoints ni acceso a datos persistentes. No afirmar persistencia real ni migración exitosa.
- [ ] Validación de migración, suites, revisión independiente y release pendientes. Ningún push/deploy realizado.

Consumidor y reglas de cálculo: [frontend](../../../gestion_fe/specs/020-cumplimiento-restricciones/tasks.md).