# 011 — Plan técnico

Pendiente de aprobación de [spec.md](spec.md).

1. `EmpresaController`: dos `@GetMapping` nuevos delegando en casos de uso (sin acceso directo a repositorios).
2. Caso de uso `GetEmpresaResumenUseCase` (`@Transactional(readOnly = true)`): `requireEmpresa`, existencia de empresa, conteos de obras y restricciones con `contarPorEmpresaRelacionada(Set.of(id))` (ya existe), suma de horas con un método nuevo `sumHorasPorEmpresaRelacionada` en `RegistroHorasRepository`, y cargas con un método nuevo en `ObraTareaRepository` que trae las tareas de la empresa con `proyecto` y `actividad` en una consulta; cada tarea se evalúa con `CreateObraTareaUseCase.toDto` para no duplicar la regla.
3. Caso de uso `GetRestriccionesEmpresaUseCase` o método nuevo en `GetRestriccionesUseCase`: `findByEmpresaRelacionadaId` nuevo en `RestriccionObraRepository`, mapeado con `RestriccionMapper`.
4. DTO `EmpresaResumenDto` (record) en `application/dto`.
5. Pruebas sin contexto (Mockito): casos de uso (conteos, empresa vacía, tarea con y sin actividad, nulos, 403/404). Prueba HTTP de autorización (Spring + H2 aislado) para empresa propia, ajena y sin sesión, siguiendo `docs/testing-checklist.md`.
6. Comandos propuestos desde `gestion_be`: `mvn -Dtest=<clases nuevas sin contexto> test` y, con aprobación de ejecución, `mvn -Dtest=<clase HTTP nueva> test`. Un `mvn verify` final solo si se aprueba.

Riesgos: la evaluación de cargas carga las actividades de cada obra (acceso perezoso); aceptable en el volumen actual, a revisar si crecen las obras. Sin migraciones; reversión: retirar los endpoints y sus clases.
