# 011 — Estado

- [x] Discovery: endpoints, DTO resumen vs detalle, regla de cumplimiento de tareas, repositorios de conteo y política `requireEmpresa`.
- [x] Especificación y plan para revisión.
- [x] Aprobación (2026-11-17): ambos endpoints, «cargas cumplidas» con la regla de `CreateObraTareaUseCase.toDto`, pruebas sin contexto y adaptación posterior del frontend. La prueba HTTP queda sin aprobar.
- [x] Implementación backend con pruebas sin contexto.
- [ ] Prueba HTTP de autorización y arranque (requiere aprobación de ejecución con H2 aislado).
- [x] Adaptación del frontend en su repositorio (ver `gestion_fe/specs/023-consultas-por-contexto/tasks.md`).
- [ ] Revisión independiente y cierre.

## Cambios

- `EmpresaController`: `GET /api/empresas/{id}/resumen` y `GET /api/empresas/{id}/restricciones`.
- Nuevos `EmpresaResumenDto`, `GetEmpresaResumenUseCase` y `GetRestriccionesEmpresaUseCase`; ambos llaman a `identity.requireEmpresa` y devuelven 404 si la empresa no existe. `TenantAccessInterceptor` ya protege cualquier ruta `/api/empresas/{id}/…`, de modo que el control existe en dos capas.
- Repositorios: `RegistroHorasRepository.sumHorasPorEmpresaRelacionada`, `ObraTareaRepository.findConActividadByEmpresaRelacionadaId` (JPQL) y `RestriccionObraRepository.findByProyectoEmpresaRelacionadaIdOrderByIdAsc` (con el mismo `@EntityGraph` que las demás lecturas). Sin cambios de esquema ni de endpoints existentes.
- `CertificadoObraUseCaseTest`: su `FakeTareas` implementa el nuevo método del repositorio.

## Verificación

| Comando (desde `gestion_be`) | Resultado |
| --- | --- |
| `mvn -q -Dtest=EmpresaResumenUseCasesTest,CertificadoObraUseCaseTest test` | 20 pruebas correctas, 0 fallos/errores/omitidas (6 nuevas + 14). Informes `target/surefire-reports` regenerados en esta ejecución. Compila todo el árbol de tests. |

Cobertura nueva: totales, cumplimiento derivado frente a la columna guardada, empresa sin datos, 403 sin lecturas, 404 y listado de restricciones con el mismo DTO que el endpoint por obra.

## Límites y riesgos

- **No se ha arrancado Spring.** Las consultas JPQL y la derivada solo se validan al crear el contexto; un error en ellas impediría el arranque de la aplicación. Los nombres de propiedades se contrastaron con las entidades y con consultas existentes, pero falta la prueba de arranque/HTTP. Es el siguiente paso recomendado y necesita aprobación (H2 aislado en memoria).
- Sin prueba HTTP de autorización: no se ha comprobado el 403 por empresa ajena con JWT real.
- El cálculo de cargas recorre las obras de la empresa con carga perezosa de actividades: el coste crece con el número de obras y tareas; sin medición.
- No se ejecutó `mvn verify` ni la suite completa.
