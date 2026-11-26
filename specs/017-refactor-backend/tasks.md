# Estado y evidencia

- [x] Descubrimiento por lectura estática de casos de uso, consumidores, pruebas y configuración Maven.
- [x] Aprobación de implementación mediante «implementalo» el 26 de noviembre de 2026. Se interpretó que la aprobación cubre el piloto propuesto y la línea base del árbol de trabajo. Alcance local en `gestion_be`: caso de uso, clase de paquete, pruebas de caracterización y documentación de seguimiento.
- [x] Piloto implementado. Pruebas de caracterización y regresión ejecutadas desde `gestion_be` con `mvn -Dtest=PrevistoRealUseCaseTest,PrevistoRealCalculatorTest,EmpresaDashboardObrasUseCaseTest,RubroEstructuraUseCasesTest test`. Las clases seleccionadas no arrancan Spring ni H2. En Java 21, con compilación objetivo Java 17, pasaron 49 pruebas sin fallos, errores ni omisiones tanto antes como después del refactor. `git diff --check` pasó.
- [ ] Revisar aislamiento y aprobar en conjunto `mvn -Dtest=PrevistoRealHttpTest test` y `mvn verify`, con reintentos dentro del mismo alcance. Aplicar el [checklist de pruebas](../../docs/testing-checklist.md) a todos los contextos incluidos y verificar H2 efímera antes de ejecutar. Prohibidos H2 persistente, operaciones destructivas y escrituras remotas.
- [ ] Completar revisión independiente y registrar comandos ejecutados, resultados, veredicto y limitaciones aceptadas.

Durante el descubrimiento se observó un diff local en el cálculo de horas previstas a fecha y días del anexo. Se conservó como línea base. Las nuevas pruebas cubren precedencia y desbordamiento de horas previstas a fecha, conservación de días, orden y ausencia de agregado sin cargas. No se inició Spring. La prueba HTTP focalizada usa un perfil con H2 en memoria identificada por UUID, validación previa a la creación del datasource y comprobación JDBC antes de cargar sus fixtures. La aprobación de ejecución sigue pendiente. La revisión de los contextos adicionales de `mvn verify` también sigue pendiente.

## Alta y edición de tareas

- [x] Localizada la duplicación en la asignación de campos y en el cálculo de métricas. Revisada la cobertura existente de pertenencia, horas, días, desencofrado y subcontratos.
- [x] Segundo corte aprobado mediante «IMPLEMENTA» el 26 de noviembre de 2026. Alcance local en `gestion_be`: `CreateObraTareaUseCase.java`, prueba de caracterización en `PrevistoRealUseCaseTest.java` y este registro. Implementada la extracción y comprobados el orden de ambos bloqueos antes de guardar y la conservación de fecha y autor de registro.
- [x] Ejecutado antes y después `mvn -Dtest=PrevistoRealUseCaseTest,PrevistoRealCalculatorTest,CertificadoObraUseCaseTest test` desde `gestion_be` con Java 21 y objetivo Java 17. En ambas ejecuciones pasaron 40 pruebas, sin fallos, errores ni omisiones. `git diff --check` pasó.
- [ ] Verificación con Spring y revisión independiente pendientes. La aprobación de implementación no cubre pruebas con contexto, operaciones sobre datos persistentes ni escrituras remotas.

## Consolidación de datos de avance

- [x] Confirmado que el frontend envía una vista previa de cantidad prevista y porcentaje. El backend persiste ambos campos y vuelve a calcularlos al responder para cargas con rubro resoluble. Los valores persistidos siguen siendo el respaldo cuando no se resuelve el rubro.
- [x] Aprobado mediante «IMPLEMENTALO» el 26 de noviembre de 2026. Alcance local en backend: caracterización y extracción hacia `ObraTareaMapper`, cambio de sus consumidores y registro de evidencia. El alcance excluye cambios de escritura y esquema, arranque de Spring, datos persistentes y escrituras remotas.
- [x] Detectada una dependencia directa del valor persistido en `DemoOperationalSeed`: suma la cantidad prevista de las tareas para generar registros horarios y consulta el porcentaje para identificar fixtures. La caracterización debe incluir ese flujo antes de cambiar escrituras.
- [x] Verificación sin Spring ni base antes y después con `mvn -Dtest=PrevistoRealUseCaseTest,PrevistoRealCalculatorTest,EmpresaResumenUseCasesTest,DemoOperationalSeedTest test` desde `gestion_be`. Pasaron 43 pruebas en ambas ejecuciones, sin fallos, errores ni omisiones, con Java 21 y objetivo Java 17. Las nuevas pruebas cubren recálculo al editar el presupuesto, nulidad con horas incompletas y respaldo para tareas sin vínculo o con rubro ausente. `git diff --check` pasó.
- [ ] Verificación HTTP y revisión independiente pendientes. La eliminación de columnas o el cambio de escritura se evalúa en un corte posterior.

## Retiro gradual de escrituras duplicadas

- [x] Revisadas las escrituras de API y del generador demo, el bloqueo de borrado de rubros con tareas en la edición de proyecto y el respaldo lógico H2. El respaldo usa metadatos y compara la huella del esquema al importar.
- [x] Corte aprobado mediante «IMPLEMENTA» el 26 de noviembre de 2026 e implementado. Archivos de código: `CreateObraTareaUseCase.java`, `DemoOperationalSeed.java` y sus pruebas unitarias. Quedan fuera migraciones, escrituras sobre datos existentes, frontend y ejecución con Spring.
- [x] Ejecutado `mvn -Dtest=PrevistoRealUseCaseTest,PrevistoRealCalculatorTest,EmpresaResumenUseCasesTest,DemoOperationalSeedTest test` desde `gestion_be` con Java 21 y objetivo Java 17. Pasaron 44 pruebas de caracterización antes del cambio y 45 en la última ejecución, sin fallos, errores ni omisiones. Las pruebas cubren respuesta, escritura y edición, respaldo previo, rechazo de cantidad inválida y totales demo. `git diff --check` pasó.
- [ ] Quedan pendientes la verificación HTTP con Spring, el ciclo completo de Maven y la revisión independiente.

## Inventario general y siguiente corte

- [x] Barrido estático de las fuentes Java principales y revisión dirigida de consumidores, tests y mappings de los candidatos.
- [x] Corte aprobado mediante «IMPLEMENTALO» el 26 de noviembre de 2026. Implementado localmente en `CreateProyectoUseCase.java`, `UpdateProyectoUseCase.java`, `DatosProyecto.java` y `RubroEstructuraUseCasesTest.java`. El alcance excluye cambios de esquema, datos persistentes, contextos Spring y escrituras remotas.
- [x] `mvn -Dtest=CreateProyectoSubcontratistaTest,RubroEstructuraUseCasesTest,ProyectoMapperTest test` ejecutado desde `gestion_be`, con Java 21 y objetivo Java 17. Pasaron 30 pruebas antes y después, sin fallos, errores ni omisiones. Las pruebas nuevas cubren la precedencia de validaciones y el tratamiento de plantas en alta y edición. `git diff --check` pasó.
- [ ] Verificación de integración y revisión independiente pendientes.
