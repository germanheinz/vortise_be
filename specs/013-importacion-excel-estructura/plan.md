# 013 — Plan técnico

Pendiente de aprobación de [spec.md](spec.md). Este plan fija decisiones técnicas (**DT-n**), archivos por repositorio y el orden de milestones. No autoriza implementar, ejecutar comandos, tocar bases ni desplegar. Las decisiones de comportamiento (DC-n) y las preguntas (P-n) están en la spec.

## Estrategia

Una funcionalidad **nueva y aislada**, en tres capas, sin tocar el importador antiguo:

1. **Dominio puro** (sin Spring ni POI): modelo del borrador, reglas de validación de celdas ya leídas, gramática y grafo de dependencias, diff, clasificación y huella. Máxima cobertura de pruebas sin contexto.
2. **Adaptadores** (infraestructura): validador de archivo `.xlsx`, lector POI, generador de plantilla, codec JSON del payload, persistencia JPA del borrador, controlador REST.
3. **Casos de uso** (aplicación): crear, leer, diff, publicar, descartar. La publicación reutiliza las mismas reglas de estructura que el guardado manual (`EstructuraRubros`, `RubroEstructura`, `RubroPlanning`, bloqueos de certificados/tareas/restricciones) para que **no existan dos fuentes de verdad**.

Alternativa descartada: ampliar `ExcelImportService` (índices fijos, otro modelo, sin borrador ni diff, crea proyectos sin empresa). Ver «Compatibilidad con el importador existente» en la spec.

## Decisiones técnicas

| # | Decisión | Alternativas y por qué no |
| --- | --- | --- |
| DT-1 | **Coexistencia**: paquetes nuevos; `ImportController`, `ExcelImportService`, `/api/import/excel`, `/import` y sus pruebas sin cambios | Reutilizar/reemplazar: rompe comportamiento no verificado y mezcla dominios (spec, «Compatibilidad») |
| DT-2 | **Lectura con POI `XSSFWorkbook` (DOM)** sobre bytes ya validados (≤ 2 MiB), una sola hoja, **solo valores en caché de fórmulas**, sin `FormulaEvaluator`. Se lee a un modelo neutro `FilaPlantilla` (strings/números/fechas/tipos de celda) y toda la validación ocurre en dominio puro | SAX/`XSSFReader`: menor memoria pero más código; innecesario con el tope de tamaño; se reevalúa si se sube el límite (P-14). Evaluar fórmulas: abre superficie (libros externos, funciones) sin beneficio |
| DT-3 | **Validación en dos niveles**: reglas por fila/celda con acumulación de todas las incidencias, y luego `EstructuraRubros.resolver` (autoritativa, lanza en la primera falla) con `tipoNodo`/`parentCodigo`/`varianteEstructura` explícitos. Una falla de `resolver` no prevista se informa como `ESTRUCTURA_INVALIDA` (red de seguridad) y se agrega una prueba | Reimplementar las reglas de árbol en el importador: riesgo de divergencia con el flujo manual |
| DT-4 | **Borrador en una tabla con payload JSON** (`importaciones_estructura`, `payload_json TEXT`): sin hijos normalizados, sin guardar el `.xlsx`. Se anula el payload al cerrar. Un payload máximo ≈ 2 MiB | Tabla normalizada de nodos: más migración y joins para una estructura que solo se lee/escribe entera. Guardar el original: más superficie y datos retenidos sin necesidad |
| DT-5 | **Un borrador activo por obra** con columna `activo_proyecto_id` (= `proyecto_id` mientras `BORRADOR`, si no `NULL`) y **índice único**, portable H2/PostgreSQL | Índice parcial (`WHERE estado='BORRADOR'`): PostgreSQL sí, H2 no; consulta previa sin índice: condición de carrera |
| DT-6 | **Diff calculado al leer**, no almacenado; `huellaDiff` = SHA-256 de una cadena canónica (ordenada) de cambios + bloqueos + id/versión del borrador. Publicar recomputa y exige coincidencia | Persistir el diff: queda obsoleto en cuanto se edite la obra |
| DT-7 | **Publicación**: nuevo `PublicarImportacionEstructuraUseCase` (`@Transactional`) que (1) bloquea la obra con `ProyectoRepository.bloquearPorId` **antes** de cualquier lectura de estructura (mismo orden de bloqueo que `UpdateProyectoUseCase`/`RegistroHorasService`, sin riesgo de interbloqueo cruzado), (2) revalida borrador, (3) recomputa el diff, (4) aplica y (5) cierra el borrador en la misma transacción. Para **REEMPLAZAR** se **extrae sin cambiar comportamiento** de `UpdateProyectoUseCase` a un colaborador de paquete `SincronizarRubrosObra` el bloque `mapRubros` + `validateStructureChanges` + guardas (`linkedIds`, `CertificadoBloqueos.validarCambioActividad`, restricciones, contratos) y el recálculo de `proyecto.horasPrevistas`; `UpdateProyectoUseCase` delega. Para **AGREGAR** no se pasa por `mapRubros`: se crean solo entidades nuevas enlazadas a padres existentes o nuevos (los existentes quedan intactos) y se valida el árbol completo con `EstructuraRubros.resolver`. Se hace `saveAndFlush` antes de marcar `PUBLICADO` para que las violaciones de restricciones aparezcan dentro de la transacción | **B**: duplicar guardas en el caso de uso nuevo (menos riesgo inmediato, deriva futura). **C**: construir un `ProyectoUpdateDto` completo y llamar a `UpdateProyectoUseCase.execute` (sobrescribiría nombre/fechas/superficies/subcontratistas de la obra y exige replicar todos los campos). Se recomienda la extracción; si el dueño no la aprueba (P-13) se usa B con pruebas de paridad |
| DT-8 | **Tenant**: la ruta `/api/proyectos/{proyectoId}/importaciones-estructura` hace que `TenantAccessInterceptor` verifique empresa/obra; el caso de uso repite `CurrentIdentity.requireEmpresa`; `@PreAuthorize("hasAnyRole('ADMINISTRADOR','JEFE_DE_OBRA')")` en el controlador; el borrador se busca por `(id, proyectoId)` | Autorizar solo por rol: no cubre multiempresa |
| DT-9 | **Errores con `code`**: nueva `ImportacionEstructuraException(code, HttpStatus, message, detalle)` y handler en `GlobalExceptionHandler` que devuelve `ImportacionErrorDto(status, error, code, message, detalle)` (precedente: `FeatureUnavailableDto`). Se reusan `SolicitudInvalidaException`/`ConflictoEstadoException` donde no hace falta `code` | Ampliar `ErrorResponseDto` global con `code`: cambia el contrato de todos los endpoints |
| DT-10 | **Configuración** `@ConfigurationProperties("app.importacion-estructura")`: `max-bytes` (2097152), `max-filas` (2000), `max-columnas` (30), `max-entradas-zip` (100), `max-descomprimido-bytes` (26214400), `ratio-maximo` (100), `draft-ttl-dias` (7), `umbral-cambio-estructural` (0.30); interruptor `app.features.importacion-estructura-enabled` leído por `ProductionFeaturePolicy` (patrón `gastos-fijos-enabled`). Valores validados al arrancar (rangos) | Constantes en código: impiden ajustar sin desplegar |
| DT-11 | **Expiración perezosa**: al leer/crear se marca `EXPIRADO` si venció; sin `@Scheduled` (no existe infraestructura de tareas programadas) | Scheduler: agrega componente operativo para un caso de baja frecuencia |
| DT-12 | **Auditoría**: columnas de cierre en la fila del borrador + logs estructurados por evento (ids, conteos, códigos de incidencia; nunca contenido de celdas) con el patrón `log.atInfo().addKeyValue(...)` ya usado en `GlobalExceptionHandler` | Tabla de auditoría genérica: no existe; fuera de alcance |
| DT-13 | **Plantilla generada** por `GeneradorPlantillaEstructura` (POI) al pedirla; el lector y el generador comparten **una sola** definición de encabezados/claves (`ColumnasPlantilla`) y hay una prueba de ida y vuelta | Binario versionado: se desincroniza del parser |
| DT-14 | **Números y fechas sin `Locale` implícito**: parseo manual determinista (regex + `BigDecimal`/`LocalDate`), conversión final a `Double` (el modelo usa `Double`); celdas de fecha por `DateUtil` de POI | `NumberFormat`/`DateTimeFormatter` con `Locale` por defecto: resultados dependientes del servidor |
| DT-15 | **Mapeo borrador → `RubroCreateDto`** (dominio/aplicación): `nRubro`=código, `tipoNodo`, `parentCodigo`, `varianteEstructura` (raíz), `nombre`, `nivel`, `cantidad`, `unidad`, `rendimientoOficial/Ayudante`, `cantidadPersonas`, fechas, `cronogramaManual` (DC-02: `true` si hay inicio y fin importados; si no, se conserva el existente). **Campos fuera de plantilla** (`subNumeroRubro`, `empresa`, `tipoContratista`, `subcontratistaId`, `frentes`, `contratoId`, y datos propios de agrupadores) se copian del nodo existente; nodos nuevos usan los valores por defecto (`PROPIA`, `frentes` nulo→1). **Celdas opcionales vacías no borran** valores existentes (APU, personas, fechas) | Tratar vacío como «borrar»: destructivo sin confirmación |
| DT-16 | **Consultas agregadas para bloqueos y diff**: evitar N+1 (hasta 2.000 nodos) añadiendo en los repositorios existentes consultas por proyecto (ids de rubros con tareas, con restricciones, con registros de horas por `numero_rubro`; completados y cortes de certificados vía `CertificadoItemCompletadoRepository.findByCertificadoProyectoId` y `CertificadoObraRepository.findByProyectoIdOrderByFechaCorteAscIdAsc`, ya usados por `CertificadoBloqueos`). Nombres exactos a confirmar al implementar | `countByRubroVinculadoId` por nodo: N consultas |
| DT-17 | **Frontend**: página nueva `proyectos/[id]/actividades/importar`, módulo `importacionEstructuraApi` en `lib/api/obras.ts` con `client` compartido, tipos en `types/index.ts`, helpers puros en `lib/obras/importacion-estructura.ts`; sin dependencias nuevas; las lecturas del borrador no pasan por la caché de negocio (formularios con datos frescos, como `getByIdFresh`) | Nueva librería de estado/formularios: no justificada |

### Algoritmo de publicación (referencia)

```
publicar(proyectoId, id, req):                       # @Transactional
  exigir interruptor activo
  proyecto = bloquearPorId(proyectoId)               # 1.º: bloqueo de la obra
  requireEmpresa(proyecto.empresa)
  borrador = buscar(id, proyectoId) o 404            # mismo 404 si es de otra obra
  si vencido -> marcar EXPIRADO -> 409 BORRADOR_EXPIRADO
  exigir estado == BORRADOR y version == req.versionBorrador
  si borrador.errores > 0 -> 409 BORRADOR_CON_ERRORES
  diff = calcular(borrador.nodos, proyecto.rubros, asociados)     # bajo el bloqueo
  si diff.huella != req.huellaDiff -> 409 DIFF_DESACTUALIZADO
  si req.modo no está en diff.modosPermitidos -> 400 CONFIRMACION_REQUERIDA
  si modo == REEMPLAZAR y diff.eliminados > 0 y req.confirmaciones.eliminaciones != diff.eliminados -> 400
  si diff.clasificacion == MUY_DIFERENTE y req.textoConfirmacion != "REEMPLAZAR" -> 400
  si diff.bloqueos no vacíos -> 409 BLOQUEOS_DE_PUBLICACION
  aplicar según modo (SincronizarRubrosObra | altas)  # mismas guardas que el flujo manual
  recalcular proyecto.horasPrevistas; saveAndFlush
  borrador.cerrarPublicado(usuario, modo, conteos); payload = null
  log import_estructura_publicado
```

Toda excepción revierte la transacción completa (cambios y estado del borrador), de modo que no hay publicación parcial. `@Version` del borrador y el bloqueo de la obra cubren publicaciones simultáneas y publicación vs. edición manual.

## Archivos y paquetes

Ruta base BE: `gestion_be/src/main/java/com/vortise/gestion/`. Todo **nuevo** salvo lo marcado *(modifica)*.

### `gestion_be` (propietario del contrato)

| Capa | Archivo / paquete | Contenido |
| --- | --- | --- |
| domain/model | `ImportacionEstructura`, `EstadoImportacionEstructura` | Entidad JPA (`importaciones_estructura`, `@Version`, `@OnDelete(CASCADE)` hacia `Proyecto`); transiciones `cerrarPublicado/Descartado/Expirado` |
| domain/repository | `ImportacionEstructuraRepository` | `findByIdAndProyectoId`, `findActivaByProyectoId`, `save`, `saveAndFlush` |
| domain/service | `importacion/` : `NodoBorrador`, `IncidenciaImportacion`, `ColumnasPlantilla`, `ValoresCelda`, `DependenciasEstructura`, `ValidadorEstructuraBorrador`, `ImportacionEstructuraDiff`, `HuellaDiff` | Lógica pura, sin Spring/POI/JPA |
| application/dto | `ImportacionEstructuraResumenDto`, `…DetalleDto`, `NodoBorradorDto`, `DependenciaDto`, `IncidenciaDto`, `…DiffDto`, `CambioNodoDto`, `CampoCambiadoDto`, `BloqueoDto`, `…PublicarRequest`, `…PublicacionDto`, `ImportacionErrorDto` | Contrato de la spec |
| application/port | `LectorPlantillaEstructura`, `GeneradorPlantillaEstructura`, `CodecPayloadImportacion` | Puertos hacia infraestructura. Para expiración y pruebas se usa un `java.time.Clock` inyectable (verificar si el proyecto ya expone uno; si no, añadirlo como bean mínimo) |
| application/exception | `ImportacionEstructuraException` | `code` + estado HTTP |
| application/usecase | `CrearImportacionEstructuraUseCase`, `GetImportacionEstructuraUseCase` (activo/detalle/diff), `PublicarImportacionEstructuraUseCase`, `DescartarImportacionEstructuraUseCase`, `ObtenerPlantillaEstructuraUseCase`, `SincronizarRubrosObra` (extraído), `RubroDesdeBorrador` (mapeo DT-15) | |
| application/usecase *(modifica)* | `UpdateProyectoUseCase` | Delegar `mapRubros`/`validateStructureChanges` a `SincronizarRubrosObra` **sin cambiar comportamiento** (DT-7; sujeto a P-13) |
| infrastructure/excel/estructura | `ValidadorArchivoXlsx`, `PoiLectorPlantillaEstructura`, `PoiGeneradorPlantillaEstructura` | Seguridad de archivo, lectura (DT-2), plantilla (DT-13) |
| infrastructure/persistence | `JpaImportacionEstructuraRepository`, `JacksonCodecPayloadImportacion` | Siguiendo el patrón de `JpaRubroObraRepository` (verificar); codec con el mapper de Jackson 3 (`tools.jackson`) |
| infrastructure/persistence *(modifica)* | Repos de tareas/restricciones/horas | Consultas agregadas de DT-16 |
| infrastructure/web | `ImportacionEstructuraController` | Rutas de la spec; `@PreAuthorize`; multipart; `Content-Disposition` de la plantilla |
| infrastructure/web *(modifica)* | `GlobalExceptionHandler` | Handler de `ImportacionEstructuraException` (fuente con cabecera «Decompiled»: cambio mínimo y revisado) |
| infrastructure/config | `ImportacionEstructuraProperties`; *(modifica)* `ProductionFeaturePolicy`, `application.yml` | Propiedades y valores por defecto de DT-10 |
| resources | `db/migration/V22__importaciones_estructura.sql` | Tabla, FK con cascada, CHECK de estado, índices; SQL portable, aditivo |
| scripts *(modifica)* | `scripts/extract-empresas.py` | Añadir `('importaciones_estructura', 'proyecto_id', 'proyectos')` a `OWNERSHIP` (H17) |
| docs | `gestion_be/docs/importacion-estructura.md` | Formato de plantilla, reglas, límites, códigos de error, operación |
| test | `…/domain/service/importacion/*Test`, `…/infrastructure/excel/estructura/*Test`, `…/application/usecase/*ImportacionEstructura*Test`, `…/infrastructure/web/ImportacionEstructuraHttpTest`, `…PostgresTest` (opt-in); ampliar `PostgresMigrationSourceTest`, `scripts/tests/test_extract_empresas.py` | Estrategia de la spec |

**No se modifican**: `ImportController`, `ExcelImportService`, `RubroCreateDto`, `RubroDto`, `ProyectoUpdateDto`, `EstructuraRubros`, `RubroPlanning`, `schema.sql`, `pom.xml` (sin dependencias nuevas), migraciones V1–V21.

### `gestion_fe`

| Archivo | Cambio |
| --- | --- |
| `types/index.ts` | Tipos espejo de la spec (DTO de borrador, diff, publicar, error) |
| `lib/api/obras.ts` | `importacionEstructuraApi`: `plantilla`, `subir`, `activo`, `detalle`, `diff`, `publicar`, `descartar` |
| `lib/obras/importacion-estructura.ts` | Helpers puros: árbol del borrador, agrupación de incidencias, habilitación de publicar, mensajes por `code`, texto de confirmación |
| `app/proyectos/[id]/actividades/importar/page.tsx` | Pantalla de 3 pasos (subir / revisar / publicar) |
| `components/proyectos/importacion/*` (p. ej. `SubirPlantilla`, `ArbolBorrador`, `IncidenciasTabla`, `DiffResumen`, `PublicarPanel`) | Componentes enfocados |
| `components/proyectos/EstructuraActividadesSection.tsx` *(modifica)* | Botón «Importar desde Excel» y aviso de borrador activo (solo con capacidad) |
| `lib/permissions.ts` *(modifica)* | `capabilities().importStructure` y `restrictedTeamRoute` para la ruta nueva |
| `tests/importacion-estructura.test.cjs` (nuevo), `tests/team-permissions.test.cjs` *(modifica)* | Pruebas `node --test` de helpers y permisos |

**No se modifican**: `app/import/page.tsx`, `lib/api/administracion.ts`, `lib/query/invalidation.ts` (ya clasifica `/api/proyectos/{id}/…` como alcance «obra»), `lib/http-client.ts`.

### Otros

- `docs/matriz-roles-permisos.md`: tras aprobar P-5, completar la fila «Importar proyecto desde Excel»/«Importar estructura» (la decisión de roles queda registrada allí).
- `gestion_be/AGENTS.md`: nota de discrepancia («no se identificó framework de migraciones» vs. Flyway V1–V21) a corregir por el dueño/arquitecto; no se cambia en esta tarea.

## Orden de milestones

Los hitos del proyecto son las 5 fases de `AGENTS.md`; los milestones técnicos M1–M7 viven dentro de la Fase 3, salvo M6 (Fase 4) y M7 (Fase 5).

| Milestone | Contenido | Salida verificable (focalizada) |
| --- | --- | --- |
| **M0** (Fase 2) | Aprobar spec y plan; resolver P-1, P-3, P-5, P-6, P-7, P-13, P-16; fijar el contrato JSON y un Excel **sintético** de referencia | Aprobaciones registradas; fixture sintético |
| **M1** Dominio puro | `ValoresCelda`, `DependenciasEstructura`, `ValidadorEstructuraBorrador`, `ImportacionEstructuraDiff`, `HuellaDiff` | Pruebas sin contexto: incidencias, bordes del umbral, ciclos, huella estable |
| **M2** Adaptadores de archivo | `ValidadorArchivoXlsx`, lector POI, generador de plantilla | Pruebas con libros sintéticos en memoria: seguridad (zip/XXE/enlaces/macros), ida y vuelta plantilla↔lector |
| **M3** Persistencia y lectura | V22 + entidad + repositorio, `CrearImportacion…`, `Get…`, `Descartar…`, controlador, handler, interruptor, `extract-empresas.py` | HTTP aislado: roles, tenant (403/404), un borrador activo, expiración, descarte, cascada; borrador no toca `rubros_obra` |
| **M4** Publicación | Extracción `SincronizarRubrosObra` (**primero** con las pruebas de regresión existentes en verde), `PublicarImportacion…`, AGREGAR/REEMPLAZAR, bloqueos, atomicidad | HTTP aislado + PostgreSQL opt-in: ids conservados, bloqueos, huella obsoleta, concurrencia |
| **M5** Frontend | Tipos, API, helpers, pantalla, permisos | `tsc`, `next build`, `node --test` |
| **M6** (Fase 4) Verificación completa | `mvn verify` (BE), `tsc` + `npm run build` + `node --test tests/*.test.cjs` (FE), verificación cruzada del contrato, **verificación manual del Cronograma** (DC-02), prueba de Flyway V22 | Informe por repositorio con comandos, resultados y omisiones |
| **M7** (Fase 5) Revisión y cierre | Revisión independiente (seguridad/tenant/atomicidad), correcciones, documentación, decisión sobre `/import` | Veredicto, evidencia y limitaciones aceptadas en `tasks.md` |

Los hitos BE (M1–M4) pueden desarrollarse en una rama de `gestion_be`; M5 en otra de `gestion_fe` tras congelar el contrato en M3. Ramas y PR separados; ningún push ni despliegue sin aprobación.

## Comandos propuestos (no ejecutados)

Desde `gestion_be` (según `AGENTS.md`; inspeccionar `pom.xml` y los contextos antes de aprobar):

```sh
mvn -Dtest=ImportacionEstructuraParserTest,ImportacionEstructuraDiffTest,DependenciasEstructuraTest,ArchivoXlsxValidatorTest test   # sin contexto
mvn -Dtest=ImportacionEstructuraHttpTest test                                  # Spring + H2 aislado: requiere aprobación de ejecución
mvn -Dtest=RubroEstructuraUseCasesTest,RubroEstructuraPersistenciaTest,EstructuraRubrosTest,RubroPlanningTest,ExcelParserSecurityTest test   # regresión (la persistencia arranca contexto: revisar aislamiento)
python3 -B -m unittest discover -s scripts/tests -p test_extract_empresas.py -v
mvn verify                                                                      # verificación final, una vez
```

Desde `gestion_fe`:

```sh
./node_modules/.bin/tsc --noEmit --incremental false
node --test tests/importacion-estructura.test.cjs tests/team-permissions.test.cjs
npm run build
```

PostgreSQL desechable (opcional, patrón de `Quality14PostgresTest`): propiedad de activación y secreto por variable de entorno, **definidos al implementar y revisados antes de ejecutar**; nunca contra la base persistente ni contra Render. No se instalan dependencias.

## Riesgos técnicos

| Riesgo | Mitigación |
| --- | --- |
| La extracción de `UpdateProyectoUseCase` altera el flujo manual | Mover código sin reescribirlo; correr primero las pruebas de regresión; revisión del diff por pares; alternativa B |
| `RubroPlanning` recalcula HH de nodos sin cambios en REEMPLAZAR | Es el comportamiento del guardado manual (el FE envía la lista completa); se documenta y se prueba |
| Orden de bloqueo (obra → borrador) distinto del de otros flujos | Siempre se bloquea primero la obra; el borrador se protege con `@Version` y no se bloquea antes de la obra |
| `TEXT`/Jackson 3/Hibernate entre H2 y PostgreSQL | Prueba `validate` en PostgreSQL; sin `@Lob`; codec aislado en un adaptador |
| Dos subidas simultáneas | Índice único; capturar `DataIntegrityViolationException` y responder 409 `BORRADOR_ACTIVO` |
| POI DOM y XML malicioso | Validación previa del contenedor, tope de tamaño, prueba con OOXML sintéticos maliciosos; no modificar `ZipSecureFile` global |
| `GlobalExceptionHandler` de origen descompilado | Cambio mínimo, localizado y cubierto por prueba |
| El H2 persistente se altera al arrancar con la entidad nueva | Tests solo en H2 en memoria; la aplicación local con `data/gestiondb` requiere aprobación específica |
| La tabla nueva rompe el script/prueba de 012 | Clasificarla en el mismo cambio |
| Drift contrato BE ↔ FE | Ejemplos JSON como fuente; prueba de contrato en BE con los mismos ejemplos; tipos TS revisados contra los records |

## Reversión

- **Código**: nuevo y aislado; revertir el PR de cada repositorio. El interruptor `app.features.importacion-estructura-enabled=false` apaga la funcionalidad sin desplegar código nuevo (503).
- **Esquema**: `V22` es aditiva (tabla nueva sin uso por el resto del sistema); revertir el código deja la tabla inofensiva. Eliminarla es un cambio destructivo que requiere aprobación aparte y una migración propia; no se edita una migración ya aplicada.
- **Datos publicados**: no hay «deshacer». La publicación se ejecuta con bloqueo y de forma atómica; en `REEMPLAZAR` las eliminaciones están acotadas a nodos sin datos asociados y confirmadas. Se recomienda respaldo previo en producción (política de respaldo existente; no se asume su configuración). Queda abierta la pregunta de si se desea una «última estructura» restaurable (fuera de alcance).
