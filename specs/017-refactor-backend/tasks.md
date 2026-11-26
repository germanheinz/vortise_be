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

## Consolidación de ProyectoMapper

- [x] «OK» interpretado como aprobación del siguiente punto del inventario. Implementación local en `ProyectoMapper.java` y caracterización en `ProyectoMapperTest.java`. Alcance limitado al mapper, las pruebas sin Spring y este registro.
- [x] `mvn -Dtest=CreateProyectoSubcontratistaTest,RubroEstructuraUseCasesTest,ProyectoMapperTest test` desde `gestion_be` con Java 21 y objetivo Java 17. Pasaron 32 pruebas antes y después, sin fallos, errores ni omisiones. Se comprobó el formato de ambas respuestas, la ausencia de lecturas de colecciones en el resumen y el orden de rubros. `git diff --check` pasó.
- [ ] Verificación de integración y revisión independiente pendientes.

## Consolidación de subcontratistas de proyecto

- [x] «HACELO» aprobó este punto del inventario. Se extrajo la resolución común en `SubcontratistasProyecto.java` y se conectó a los casos de uso de alta y edición con sus constructores existentes. Alcance local, sin cambios de datos, esquema o ejecución con Spring.
- [x] `mvn -Dtest=CreateProyectoSubcontratistaTest,RubroEstructuraUseCasesTest,ProyectoMapperTest test` desde `gestion_be`. Pasaron 34 pruebas antes y después, sin fallos, errores ni omisiones. Un intento intermedio falló por una anotación duplicada y se corrigió. Las nuevas pruebas cubren duplicados antes de consultar el catálogo, selección implícita y protección contra retirar un subcontratista asignado. `git diff --check` pasó.
- [ ] Verificación de integración y revisión independiente pendientes.

## Revisión de la doble lectura de avances

- [x] La primera consulta identifica rubros vinculados y la segunda alimenta la validación de certificados. Ambas preceden a la modificación de rubros. El bloqueo pesimista del proyecto no establece por sí solo que la consulta de tareas tenga el mismo resultado en ambos puntos. El alta y la edición de tareas no adquieren explícitamente ese bloqueo.
- [ ] Conservar las dos lecturas hasta verificar la semántica concurrente en los perfiles de base utilizados. Se puede abordar por separado la repetición de búsquedas de rubros en el DTO y conservar el orden y los errores. La revisión fue estática.

## Cálculo común de horas reales

- [x] Aprobado mediante «IMPLEMENTEMOS» el 26 de noviembre de 2026. Alcance local en `RegistroHorasRules.java`, `RegistroHorasService.java`, `UpdateProyectoUseCase.java` y `Quality14RulesTest.java`. Se conservaron bloqueo, sincronización, guardado, esquema y contrato de API.
- [x] Ejecutado `mvn -Dtest=Quality14RulesTest,RubroEstructuraUseCasesTest test` desde `gestion_be` con Java 21 y objetivo Java 17. Pasaron 38 pruebas antes y después, sin fallos, errores ni omisiones. Se comprobaron entrada manual, normalización de agregado nulo, redondeo, sincronización antes de sumar y baja del último registro. `git diff --check` pasó.
- [ ] Verificación de integración y revisión independiente pendientes.

## Migración de arquitectura

- [x] Rama local `refactor` creada desde `main`. La solicitud de implementar la arquitectura se tomó como aprobación del primer corte de escritura de Registro de horas, propuesto inmediatamente antes. Alcance de código: controller, tres delegadores y pruebas focalizadas sin contexto. Los cortes posteriores quedan sujetos a su alcance aprobado.
- [x] `RegistroHorasController` llama directamente a `RegistroHorasService` en alta, edición y baja. Eliminadas las tres clases delegadoras y comprobada la ausencia de referencias. Rutas, DTO, anotaciones de acceso, lógica y demarcación transaccional conservados.
- [x] Antes del cambio, `mvn -Dtest=Quality14RulesTest,RubroEstructuraUseCasesTest test` pasó 38 pruebas. Después, `mvn -Dtest=RegistroHorasControllerTest,Quality14RulesTest,RubroEstructuraUseCasesTest test` pasó 39 pruebas, sin fallos, errores ni omisiones. La prueba del controller verificó delegación, cuerpo y códigos 201, 200 y 204 sin arrancar Spring. `git diff --check` pasó.
- [ ] Aprobar los cortes de consultas y módulos siguientes descritos en el plan. La verificación de integración y la revisión independiente siguen pendientes.

### Consolidación de módulos

- [x] «completa el refactor de la arquitectura» autoriza continuar la migración del plan por módulos en `refactor`. Se conservan rutas, guardas, límites transaccionales, reglas y contratos. El alcance de ejecución sigue limitado a pruebas sin contexto.
- [x] Servicios consolidados para registros horarios, tareas, restricciones, comentarios, gastos fijos, consultas de proyectos y empresas. Desde el inicio de este corte se retiraron 22 clases y se añadieron 6. Se mantienen repositorios y adaptadores.
- [x] Comparación estática contra `HEAD` de anotaciones HTTP y firmas `ResponseEntity` en RegistroHoras, ObraTarea, GastoFijo, Restriccion, Empresa y Proyecto. Se revisó la demarcación transaccional por método. `git diff --check` pasó.
- [x] Ejecutado `mvn -Dtest=RegistroHorasControllerTest,Quality14RulesTest,RubroEstructuraUseCasesTest,PrevistoRealUseCaseTest,GastoFijoAvailabilityTest,RestriccionAltaTest,RestriccionEdicionTest,UpdateRestriccionJefeRulesTest,ConsultarRestriccionesObraUseCaseTest,EmpresaTenantScopeTest,ProyectoDemoResetAvailabilityTest,ProyectoMapperTest,CreateProyectoSubcontratistaTest,CertificadoObraUseCaseTest,EmpresaResumenUseCasesTest,EmpresaDashboardObrasUseCaseTest test` desde `gestion_be`. Pasaron 122 pruebas sin fallos, errores ni omisiones. Durante la migración se corrigieron referencias antiguas en pruebas tras dos fallos de compilación.
- [ ] Integración con Spring y revisión independiente pendientes. Su ejecución y cierre siguen las aprobaciones del backend.

### Paquetes de Empresa

- [x] Movidos `EmpresaMapper` y `EmpresaService` a `application.mapper` y `application.service` por indicación del usuario. Actualizadas las referencias de código productivo y pruebas. El alcance preserva API, reglas, repositorios y transacciones.
- [x] Ejecutado `mvn -Dtest=EmpresaTenantScopeTest,EmpresaResumenUseCasesTest,EmpresaDashboardObrasUseCaseTest test` desde `gestion_be`. `git diff --check` pasó. Queda pendiente la integración con Spring.

### Organización completa de paquetes

- [x] Aprobada mediante «hacé lo mismo para todos los demás clases». Las 60 clases del antiguo paquete quedaron distribuidas en 42 servicios, 5 mappers y 13 auxiliares. Se actualizaron los consumidores y se movieron 37 clases de prueba. Alcance local en `refactor`, sin arranque de Spring ni cambios remotos.
- [x] Comparados los cuerpos de las 60 clases con `HEAD` ignorando paquetes, imports, visibilidad y formato. No quedaron referencias al paquete anterior en fuentes ni configuración Maven. `git diff --check` pasó.
- [x] `mvn '-Dtest=com/vortise/gestion/application/**/*Test,RegistroHorasControllerTest,GastoFijoAvailabilityTest,ProyectoDemoResetAvailabilityTest,DemoOperationalSeedTest' clean test` desde `gestion_be`. Pasaron 319 pruebas sin fallos, errores ni omisiones. La compilación limpia confirmó las referencias entre paquetes y corrigió los fallos intermedios de la migración.
- [ ] Integración con Spring y revisión independiente pendientes.

### Nombres de servicios y reglas de dominio

- [x] Aprobado mediante «HACELO». Se renombraron 28 servicios y sus consumidores y se movió `EstructuraRubros` con su prueba a `domain.rule`. Se retiró `CostoCalculadorService` tras comprobar que no tenía referencias en el código ni configuración versionada. Se conservaron los puertos y las rutas, permisos y transacciones actuales.
- [x] Ejecutado `mvn '-Dtest=com/vortise/gestion/application/**/*Test,EstructuraRubrosTest,RegistroHorasControllerTest,GastoFijoAvailabilityTest,ProyectoDemoResetAvailabilityTest,DemoOperationalSeedTest' clean test` y repetida la selección con `test` tras ajustar los nombres de variables en controllers. Pasaron 319 pruebas en ambas ejecuciones. `git diff --check` pasó y no quedaron referencias a nombres `UseCase` ni al paquete `domain.service` en producción.
- [ ] Integración con Spring y revisión independiente pendientes. Alcance local.

### Retiro de seeders de negocio demo

- [x] Revisadas referencias productivas y de pruebas. Veintiséis fuentes de pruebas referencian seeders o fixtures demo y requieren distinguir pruebas exclusivas del demo de mocks usados por pruebas de producción. `PlanSuscripcionSeeder` y `SuperAdminSeeder` dependen de la política de arranque, y el lanzador E2E del frontend depende del perfil `e2e-local`.
- [x] «apruebo» autoriza el retiro del arranque y reset demo en backend. La retirada del módulo de gastos fijos y la relocalización de fixtures E2E requieren cambios coordinados con frontend. El frontend tiene cambios locales de autenticación fuera de este alcance.
- [x] Retiradas cinco clases de demo en producción, siete archivos de pruebas exclusivos del demo y el endpoint de reset. Eliminadas las referencias a seeders y a la opción de reset de las pruebas productivas y perfiles. Se preservaron los hooks operativos, los fixtures E2E y los modelos de datos.
- [x] Ejecutado `mvn -Dtest=ProductionFeaturePolicyTest,ProductionConfigurationTest,StagingSeedRunPolicyTest,SeedersSkipOnInitializedDatabaseTest,GastoFijoAvailabilityTest,PlantaTestIsolationPolicyTest test`. `git diff --check` pasó. La compilación requirió actualizar el constructor de `InvitacionUsuarioServiceTest` con un mock de Clerk. Una ejecución ampliada detectó fallos de esa suite, reproducidos en una exportación limpia de `HEAD` con la misma adaptación de compilación.
- [ ] Verificación de integración con Spring y revisión independiente pendientes. La aprobación excluye datos persistentes, ejecución con Spring, frontend y escrituras remotas.

### Retiro de Telegram y del alta por chat

- [x] Descubrimiento en ambos repositorios. El asistente web usa GET y POST sobre la misma ruta. El relay de Telegram también dispone de un workflow versionado en frontend.
- [x] «apruebo» autorizó el corte coordinado. Alcance: rutas y servicios exclusivos de Telegram y alta por chat, sus pruebas, el modo de alta del asistente y el workflow versionado. Quedan fuera operaciones remotas y persistentes. Las historias Git se mantienen separadas.
- [x] Backend: retirados ocho archivos Java productivos y tres pruebas exclusivas de Telegram. Se conservan los mappings históricos para restauración. Comparados los nueve métodos HTTP restantes de RestriccionController con HEAD; coinciden. `mvn -Dtest=RestriccionAltaTest,RestriccionEdicionTest,UpdateRestriccionJefeRulesTest,RestriccionObraConsultaServiceTest,WriteEndpointsGuardTest clean test` pasó 23 pruebas. `git diff --check` pasó.
- [x] Frontend: retirados el modo de alta, su cliente HTTP y el workflow de Telegram. Las dos acciones de consulta y el workflow simulado se conservan. `node --test tests/asistente-contexto.test.cjs tests/n8n-workflows.test.cjs` pasó tres pruebas. `git diff --check` pasó para los archivos del corte; el checkout conserva espacios finales en el cambio ajeno de la invitación.
- [ ] La comprobación completa de frontend está bloqueada por TS2339 en `app/invitacion-usuario/page.tsx`, reproducido en una copia con los cambios de autenticación preexistentes y sin este corte. `npm run build` falló primero por la restricción de puerto de Turbopack; el reintento `npm run build -- --webpack` compiló y llegó al mismo error de TypeScript. No se ejecutaron contextos de Spring ni se operó sobre n8n, Render o datos persistentes.

### Ordenar controllers y pagos

- [x] «implementa» autorizó el traslado local de controllers y servicios de pagos. Se movieron 25 controllers y 3 servicios. La comparación con HEAD confirmó que los archivos movidos coinciden fuera de paquetes e imports. Se actualizó el escaneo de guardas. Documenta el método existente de aceptación pública de invitaciones mediante una excepción acotada en la prueba.
- [x] `mvn -Dtest=WriteEndpointsGuardTest,StripeWebhookServiceTest,SuscripcionServiceTest,PaymentConfigurationControllerTest,RegistroHorasControllerTest,RestriccionAltaTest,RestriccionObraConsultaServiceTest clean test` pasó 35 pruebas sin fallos, errores ni omisiones. `git diff --check` pasó.
- [ ] Integración con Spring y revisión independiente pendientes. Alcance local en `refactor`, sin cambios de proveedor ni entorno.

### Unificar servicios y separar responsabilidades HTTP y de Clerk

- [x] «apruebo» autorizó la reorganización local de paquetes del backend y las comprobaciones sin arranque de Spring.
- [x] Se movieron 43 clases productivas y se actualizaron paquetes, imports y pruebas afectadas. La sincronización de usuarios está en service/usuario, la autenticación de Clerk en infrastructure/security/clerk, el manejador de errores en controller/advice y el filtro en infrastructure/observability.
- [x] `mvn -Dtest=WriteEndpointsGuardTest,StripeWebhookServiceTest,SuscripcionServiceTest,AutenticacionServiceTest,UsuarioObrasAsignadasTest,RequestObservationFilterTest,FeatureUnavailableResponseTest,JwtAuthenticationFilterTest,ProductionConfigurationTest,RegistroHorasControllerTest clean test` pasó 56 pruebas. `git diff --check` pasó.
- [ ] Integración con contexto de Spring y revisión independiente pendientes.

### Consolidación de controllers de invitación

- [x] «apruebo» autorizó consolidar los cuatro controllers activos del backend. El retiro de la API legada depende de confirmar su uso en producción.
- [x] Creado un controller administrativo y uno público. Actualizada la prueba de guardas para reconocer el controller público.
- [x] La comparación estática con HEAD confirmó los once contratos de verbo, ruta y guarda. Pasaron `mvn -Dtest=WriteEndpointsGuardTest clean test` y `git diff --check`.
- [ ] Integración con contexto de Spring, revisión independiente y verificación de tráfico legado pendientes.

### Retiro de entidades residuales de Telegram y alta por chat

- [x] «elimina todo lo relacionado» autoriza retirar las entidades residuales del código. Las migraciones y los datos históricos se conservan.
- [x] Eliminadas las dos entidades y revisadas las referencias. El respaldo H2 incluye ambas tablas históricas cuando existen.
- [x] Pasaron `mvn -Dtest=RestriccionAltaTest,RestriccionEdicionTest,RestriccionObraConsultaServiceTest,WriteEndpointsGuardTest clean test` y `git diff --check`.
- [ ] Pendientes la prueba de exportación y restauración con Spring y la revisión independiente.

### Retiro de PerfilController y fotos de perfil

- [x] Identificados consumidores de `/api/me` en avatar, planes y administración. FotoPerfilStorage se comparte con autenticación y su TipoImagen con reportes.
- [ ] Aprobación del corte coordinado backend/frontend y del contrato de compatibilidad.
- [ ] Implementación, pruebas focalizadas y revisión.

- [x] «apruebo» autorizó el corte local en ambos repositorios. Se eliminó el almacenamiento de fotos de perfil, se mantuvo GET /api/me autenticado en AuthController y se extrajo TipoImagen para reportes.
- [x] Backend: `mvn -Dtest=AutenticacionServiceTest,ReporteObraServiceTest,CloudinaryImagenReporteStorageTest,SoloLecturaWriteGuardTest,WriteEndpointsGuardTest,E2eLocalConfigurationTest clean test` pasó 47 pruebas.
- [x] Frontend: `node --test tests/context-queries.test.cjs` pasó 16 pruebas. `git diff --check` pasó en backend y en los archivos editados de frontend.
- [ ] El chequeo TypeScript del frontend devuelve el TS2339 preexistente de Clerk en `app/invitacion-usuario/page.tsx`. La verificación HTTP con Spring y la revisión independiente siguen pendientes.

### Consolidación de restricciones

- [x] «implementalo» autorizó consolidar los tres servicios de restricciones en el backend. La revisión de los demás servicios quedó documentada en el plan.
- [x] RestriccionController depende de RestriccionService. El cálculo gerencial se prueba como función sin Spring.
- [x] Pasaron 49 pruebas con `mvn -Dtest=RestriccionAltaTest,RestriccionEdicionTest,UpdateRestriccionJefeRulesTest,RestriccionResumenGerencialTest,RubroEstructuraUseCasesTest,WriteEndpointsGuardTest test`. Pasó `git diff --check`.
- [ ] Verificación HTTP con Spring y revisión independiente pendientes.

### Consolidación de servicios de empresa

- [x] «implementa» autorizó la consolidación de los cuatro grupos propuestos en el backend local.
- [x] Retirados cinco servicios y creado EmpresaConsultaService. Las 27 rutas de ambos controllers conservan sus declaraciones HTTP y guardas, comparadas con HEAD.
- [x] Pasaron 43 pruebas con `mvn -Dtest=CompanyInitializationTest,EmpresaTenantScopeTest,EmpresaResumenUseCasesTest,EmpresaDashboardObrasUseCaseTest,RegistroServiceTest,RestriccionAltaTest,RestriccionEdicionTest,UpdateRestriccionJefeRulesTest,WriteEndpointsGuardTest clean test`. Pasó `git diff --check`.
- [ ] Integración con contexto de Spring y revisión independiente pendientes.

### Retiro de gastos fijos globales

- [x] «eliminalo» autorizó el cambio local de backend y frontend, con preservación de datos históricos.
- [x] Retirados la pantalla, API, cliente, entidad, repositorios, DTO, política exclusiva y pruebas obsoletas. El respaldo H2 conserva la tabla física si existe.
- [x] Backend: 15 pruebas con `mvn -Dtest=WriteEndpointsGuardTest,FeatureUnavailableResponseTest,AdminBackupDisabledControllerTest,PostgresMigrationSourceTest,E2eLocalConfigurationTest,ProductionConfigurationTest clean test`. Frontend: 17 pruebas con `node --test tests/asistente-contexto.test.cjs tests/context-queries.test.cjs`. Pasó `git diff --check` en el alcance.
- [ ] Pendientes integración con Spring y revisión independiente. El catálogo histórico de planes y las migraciones aplicadas conservan sus textos.

### Consolidación de consultas y baja de proyectos

- [x] El pedido de refactor autorizó el candidato ya identificado de baja de proyecto. Se consolidó con las consultas en ProyectoService.
- [x] Pasaron 42 pruebas con `mvn -Dtest=ProyectoServiceTest,WriteEndpointsGuardTest,CreateProyectoSubcontratistaTest,CalendarioLaboralObraTest,RubroEstructuraUseCasesTest,EmpresaTenantScopeTest clean test`. Pasó `git diff --check`.
- [ ] Verificación HTTP con Spring y revisión independiente pendientes. Los demás candidatos documentados requieren un alcance aprobado antes de su implementación.

### Alta y datos comunes de proyecto

- [x] «mismo patron» autorizó continuar la consolidación de proyectos. Alta movida a ProyectoService; superficies y calendario explícito compartidos con edición.
- [x] Pasaron 37 pruebas con `mvn -Dtest=ProyectoServiceTest,WriteEndpointsGuardTest,CreateProyectoSubcontratistaTest,CalendarioLaboralObraTest,RubroEstructuraUseCasesTest clean test`. Pasó `git diff --check`.
- [ ] Pendientes integración con Spring y revisión independiente.
