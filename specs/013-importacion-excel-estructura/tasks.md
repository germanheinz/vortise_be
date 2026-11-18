# 013 — Estado

Estado de [spec.md](spec.md) y [plan.md](plan.md): **propuesta pendiente de aprobación** (2026-11-17). Todas las tareas de implementación están **pendientes**; nada se ejecutó: ni comandos, ni pruebas, ni migraciones, ni acceso a bases, ni despliegues. Este archivo registra secuencia y evidencia; no la sustituye. Las 5 fases siguen `AGENTS.md` (descubrimiento/especificación → plan aprobado → implementación con comprobaciones focalizadas → verificación completa → revisión independiente y cierre).

Leyenda: `[x]` hecho y respaldado por evidencia citada · `[ ]` pendiente. Responsable: **BE** = `gestion_be`, **FE** = `gestion_fe`, **Dueño** = decisión/aprobación.

## Fase 1 — Descubrimiento y especificación

- [x] Discovery estático del código (BE y FE): estructura `RubroObra`, validación de 3 capas, flujo de guardado manual, importador existente, seguridad de tenant, esquema/migraciones, cálculo de cronograma, convenciones de errores y pruebas. Resultado: hechos H1–H18 de la spec. *Evidencia: lectura de código; sin ejecución.*
- [x] Spec, plan y tasks redactados (este conjunto de documentos).
- [ ] **Dueño**: revisar H1–H18 (en particular H1 Boot 4.1.2 y H10 importador sin empresa) y confirmar que el formato de la plantilla coincide con el Excel real. Criterio: P-16 resuelto con un Excel **sintético** de ejemplo.
- [ ] **Dueño**: responder las preguntas bloqueantes P-1, P-3, P-5, P-6, P-7, P-13 (resto con valor recomendado si no hay objeción explícita registrada).

## Fase 2 — Plan aprobado

- [ ] **Dueño**: aprobar la spec (DC-01…DC-04, umbral P-4, roles P-5, TTL P-9, límites P-14). Criterio: aprobación registrada con alcance; el silencio no cuenta.
- [ ] **Dueño**: aprobar el plan (DT-1…DT-17), incluida la extracción de `UpdateProyectoUseCase` (DT-7/P-13) o la alternativa B.
- [ ] **Dueño**: aprobar el alcance de ejecución para la Fase 3/4: comandos focalizados, `mvn` con H2 aislado, PostgreSQL desechable opcional, `node --test`, `tsc`, `next build`. Migraciones sobre bases persistentes **no** incluidas: requieren aprobación aparte.
- [ ] **Arquitecto**: congelar el contrato JSON (ejemplos de la spec) como referencia de pruebas de contrato y crear el libro sintético de referencia (válido, con errores y con dependencias).

## Fase 3 — Implementación con comprobaciones focalizadas

### M1 — Dominio puro (BE)

- [ ] T3.1 `ColumnasPlantilla` + normalización de encabezados. *Criterio:* tildes/mayúsculas/espacios indiferentes; claves únicas; usada por lector y generador.
- [ ] T3.2 `ValoresCelda`: números es-AR (`12,5`, `1.234,56`, ambiguos `1.234`), códigos (numérico no entero → `CODIGO_NUMERICO_AMBIGUO`), fechas `dd/MM/yyyy` estrictas, límites de rango. *Criterio:* tabla de casos de la spec en pruebas; sin `Locale` implícito.
- [ ] T3.3 `DependenciasEstructura`: gramática, resolución, `Inicio de obra`, auto-referencia, ciclos con camino, expansión de agrupadores. *Criterio:* ejemplos de la spec (`3.1.2, 5.1, 5.2`; `3.2.1 (+10 d)`; `Inicio de obra`) y ciclo A→B→C→A.
- [ ] T3.4 `ValidadorEstructuraBorrador`: todas las incidencias de la tabla, inferencia de variante, llamada a `EstructuraRubros.resolver` con `tipoNodo`/`parentCodigo`/`varianteEstructura` explícitos, totales recalculados (DC-01), `RubroPlanning` coherente. *Criterio:* una prueba por código de incidencia; paridad con el flujo manual (mismas filas `RubroObra` resultantes).
- [ ] T3.5 `ImportacionEstructuraDiff` + `HuellaDiff`: A/S/M/R, Me/Md, cambio estructural, clasificación, modos, bloqueos, celdas opcionales vacías que no borran. *Criterio:* casos 29,9 % / 30 %; huella estable y sensible a cualquier cambio.
- [ ] T3.6 Verificación focalizada M1: `mvn -Dtest=ImportacionEstructuraParserTest,ImportacionEstructuraDiffTest,DependenciasEstructuraTest test` (sin contexto). *Criterio:* resultado real registrado abajo.

### M2 — Adaptadores de archivo (BE)

- [ ] T3.7 `ValidadorArchivoXlsx`: extensión, firma, tamaño, recorrido ZIP acotado (entradas, descomprimido, ratio, rutas), enlaces externos, macros, incrustados. *Criterio:* cada rechazo de la spec con libro sintético; no se tocan los límites globales de POI.
- [ ] T3.8 `PoiLectorPlantillaEstructura`: localizar cabecera (filas 1–10), mapear por clave, valores en caché de fórmulas, celdas de error, filas ocultas/combinadas, límites de filas/columnas/texto. *Criterio:* reordenar columnas o mover la cabecera no cambia el resultado.
- [ ] T3.9 Pruebas de seguridad OOXML: DOCTYPE con entidad externa y «billion laughs» sintéticos. *Criterio:* rechazo controlado sin lectura externa ni consumo excesivo (evidencia de que POI 5.5.1 los bloquea, hoy **no verificado**).
- [ ] T3.10 `PoiGeneradorPlantillaEstructura` + prueba de ida y vuelta (la plantilla generada se lee sin errores y sin filas). *Criterio:* encabezados exactos, columna Código como texto, hojas `Estructura`/`Ejemplo`/`Instrucciones`.

### M3 — Persistencia, lectura y descarte (BE)

- [ ] T3.11 `V22__importaciones_estructura.sql` (aditiva, portable) + entidad `ImportacionEstructura` + repositorio. *Criterio:* tabla, FK con cascada, CHECK de estado, índice único de `activo_proyecto_id`; `ddl-auto: validate` acepta la entidad en PostgreSQL (prueba opt-in); aserción de `V22` en `PostgresMigrationSourceTest`.
- [ ] T3.12 `scripts/extract-empresas.py`: clasificar `importaciones_estructura` en `OWNERSHIP`. *Criterio:* `test_extract_empresas.py` sigue verde.
- [ ] T3.13 Propiedades `app.importacion-estructura.*` + interruptor `app.features.importacion-estructura-enabled` (`ProductionFeaturePolicy`). *Criterio:* validación de rangos al arrancar; apagado ⇒ 503 `FEATURE_TEMPORARILY_UNAVAILABLE`.
- [ ] T3.14 Casos de uso crear/activo/detalle/diff/descartar/plantilla + `ImportacionEstructuraController` + handler `ImportacionEstructuraException` en `GlobalExceptionHandler`. *Criterio:* contrato JSON idéntico a los ejemplos; un borrador activo; expiración perezosa; descarte idempotente.
- [ ] T3.15 Autorización y aislamiento: `@PreAuthorize` (ADMINISTRADOR, JEFE_DE_OBRA), 403 entre empresas, 404 idéntico para borrador de otra obra. *Criterio:* `ImportacionEstructuraHttpTest` por rol × endpoint (checklist `docs/testing-checklist.md`).
- [ ] T3.16 Auditoría: columnas de cierre + logs estructurados sin contenido de celdas. *Criterio:* prueba de que errores y logs no contienen valores de celdas ni trazas.
- [ ] T3.17 Verificación focalizada M3: `mvn -Dtest=ImportacionEstructuraHttpTest test` (requiere aprobación de ejecución del contexto). *Criterio:* el borrador no altera `rubros_obra`; registrar resultado.

### M4 — Publicación (BE)

- [ ] T3.18 **Extraer** `SincronizarRubrosObra` desde `UpdateProyectoUseCase` sin cambiar comportamiento (si se aprueba P-13). *Criterio:* `RubroEstructuraUseCasesTest`, `RubroEstructuraPersistenciaTest`, `EstructuraRubrosTest`, `RubroPlanningTest` verdes **antes** de seguir.
- [ ] T3.19 `RubroDesdeBorrador` (DT-15): campos fuera de plantilla preservados; `cronogramaManual` según DC-02; celdas opcionales vacías conservan valores.
- [ ] T3.20 `PublicarImportacionEstructuraUseCase`: bloqueo de la obra, revalidación, huella, modos AGREGAR/REEMPLAZAR, confirmaciones, bloqueos, recálculo de `horasPrevistas`, cierre del borrador, `saveAndFlush`. *Criterio:* criterios de aceptación 9–16 de la spec.
- [ ] T3.21 Consultas agregadas de bloqueos (DT-16). *Criterio:* sin N+1 con 2.000 nodos (verificación por conteo de consultas en prueba).
- [ ] T3.22 Pruebas de atomicidad (fallo inyectado) y de concurrencia: publicación doble, publicación vs. edición manual, subida concurrente. *Criterio:* H2 para lógica; PostgreSQL desechable opt-in para bloqueo/índice único.
- [ ] T3.23 Verificación focalizada M4 (comandos del plan). *Criterio:* resultados reales registrados.

### M5 — Frontend (FE; tras congelar el contrato en M3)

- [ ] T3.24 Tipos en `types/index.ts` y `importacionEstructuraApi` en `lib/api/obras.ts`. *Criterio:* alineados con los records y ejemplos de la spec; `tsc` sin errores.
- [ ] T3.25 `lib/obras/importacion-estructura.ts` + `tests/importacion-estructura.test.cjs`. *Criterio:* árbol, incidencias, habilitación de «Publicar», mensajes por `code`, texto de confirmación `REEMPLAZAR`.
- [ ] T3.26 Pantalla `proyectos/[id]/actividades/importar` (subir / revisar: árbol-incidencias-diff / publicar / descartar) con estados de carga, vacío, error y éxito; diálogo `MUY_DIFERENTE`; manejo de `BORRADOR_ACTIVO`, `DIFF_DESACTUALIZADO`, 403/404/413/503. *Criterio:* criterio de aceptación 24.
- [ ] T3.27 Entrada «Importar desde Excel» + aviso de borrador activo en Estructura de actividades; `capabilities().importStructure` y `restrictedTeamRoute`; ampliar `tests/team-permissions.test.cjs`. *Criterio:* colaborador no ve ni accede; el backend sigue siendo la autoridad.
- [ ] T3.28 Verificación focalizada FE: `./node_modules/.bin/tsc --noEmit --incremental false` y `node --test tests/importacion-estructura.test.cjs tests/team-permissions.test.cjs`. *Criterio:* resultados reales registrados. Antes de escribir código Next, leer la guía vigente en `node_modules/next/dist/docs/` (AGENTS FE).

## Fase 4 — Verificación completa

- [ ] T4.1 BE: una `mvn verify` completa. *Criterio:* BUILD SUCCESS y conteos registrados; fallos y omisiones informados.
- [ ] T4.2 BE: `python3 -B -m unittest discover -s scripts/tests -p test_extract_empresas.py -v` y prueba PostgreSQL opt-in (V22 + `validate` + concurrencia), si fue aprobada.
- [ ] T4.3 FE: `./node_modules/.bin/tsc --noEmit --incremental false`, `node --test tests/*.test.cjs` y `npm run build`. *Criterio:* resultados reales registrados.
- [ ] T4.4 Contrato BE ↔ FE: respuestas reales vs. ejemplos y tipos TS, campo a campo. Informe por repositorio por separado.
- [ ] T4.5 **Verificación manual de Cronograma/Gantt** tras una publicación con fechas (DC-02, criterio 22): las barras muestran el rango importado y no se recalcula el fin en silencio.
- [ ] T4.6 Regresión del importador antiguo: `ExcelParserSecurityTest`, `context-queries.test.cjs` y rutas `/api/import/excel` y `/import` sin cambios (criterio 25).
- [ ] T4.7 (Opcional, si el dueño lo aprueba) recorrido E2E Playwright subir → revisar → publicar con datos sintéticos.

## Fase 5 — Revisión independiente y cierre

- [ ] T5.1 Revisión independiente (no el implementador): seguridad de archivo, aislamiento tenant, atomicidad/concurrencia, fidelidad de la extracción de `UpdateProyectoUseCase`, contrato BE ↔ FE.
- [ ] T5.2 Corregir hallazgos y repetir solo las comprobaciones afectadas (misma aprobación de alcance).
- [ ] T5.3 Documentación: `gestion_be/docs/importacion-estructura.md`; completar la fila de importación en `docs/matriz-roles-permisos.md`; registrar la discrepancia de `gestion_be/AGENTS.md` sobre Flyway.
- [ ] T5.4 Decisión del dueño sobre el interruptor en producción (P-17) y sobre el futuro de `/import` (P-11, spec aparte).
- [ ] T5.5 Cierre: veredicto, evidencia, comandos ejecutados, resultados, omisiones y limitaciones aceptadas en este archivo.

## Verificación ejecutada

Ninguna. (Se completará con comandos, fecha, entorno, resultados y omisiones; no se acepta evidencia indirecta como sustituto.)

## Bloqueos y aprobaciones abiertas

| Bloqueo | Quién | Impide |
| --- | --- | --- |
| Excel sintético de referencia (P-16) | Dueño | Cerrar la spec |
| Roles (P-5), cronograma (P-6), dependencias (P-7), refactor (P-13), semántica de nombre de hoja (P-1) y de «Tipo de APU» (P-3) | Dueño | Iniciar Fase 3 |
| Alcance de ejecución de pruebas (H2 aislado, PostgreSQL desechable, `node --test`, `tsc`, `next build`) | Dueño | Fases 3 (focalizadas) y 4 |
| Cualquier migración sobre una base persistente o despliegue | Dueño | Fuera de este conjunto de tareas |

## Límites de este documento

Solo descubrimiento estático. No se leyeron datos reales ni bases, no se abrieron `PREV VS REAL.xlsx` ni `VÓRTISE OS (1).pdf`, no se ejecutó ningún comando de compilación o prueba y no se verificó en ejecución ningún comportamiento (en particular H8, H10 y la compatibilidad de `TEXT`/Jackson 3 entre H2 y PostgreSQL).
