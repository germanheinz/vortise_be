# 009 — Estado y evidencia backend

## Área asociada a responsables (aprobada 2026-11-01)

- [x] Entidad y DTO extienden el área opcional, con límite de 120 caracteres y constructores compatibles.
- [x] Use case normaliza/persiste el área; PUT conserva el nuevo campo.
- [x] V14 y schema.sql H2 contienen cambios aditivos; migración no aplicada.
- [x] Pruebas fuente para creación, edición, pertenencia empresarial y V14.
- [x] Diagnósticos editoriales de fuentes y prueba unitaria de use case sin errores.
- [ ] Maven/tests, aplicación de migración, revisión independiente y release no ejecutados.

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