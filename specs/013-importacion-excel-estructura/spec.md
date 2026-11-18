# 013 — Importación de la estructura de obra desde Excel (plantilla fija, con borrador)

Estado: **propuesta pendiente de aprobación** (redactada 2026-11-17). **Sin código, sin ejecución de comandos o pruebas, sin lectura de datos ni de bases.** Repositorios afectados: `gestion_be` (propietario del contrato, parser, borrador, diff y publicación) y `gestion_fe` (pantalla de importación). Historias Git independientes; el contrato de este documento es la referencia compartida. Marco: [AGENTS.md](../../../AGENTS.md), [gestion_be/AGENTS.md](../../AGENTS.md), [gestion_fe/AGENTS.md](../../../gestion_fe/AGENTS.md). Relacionado con [ESTRUCTURA-3-CAPAS-18](../../../docs/specs/ESTRUCTURA-3-CAPAS-18/spec.md) (modelo de 3 capas, que dejó la importación desde planilla fuera de alcance) y con [010-cronograma-laboral](../010-cronograma-laboral/spec.md). Plan técnico: [plan.md](plan.md). Secuencia y estado: [tasks.md](tasks.md).

Este documento no autoriza implementar. Las decisiones marcadas **DC-n** (comportamiento) y las preguntas **P-n** (al final) requieren aprobación expresa del dueño; el silencio no es aprobación.

## Objetivo

Permitir que un usuario autorizado cargue en una obra existente la estructura de actividades (rubros, subrubros, niveles y actividades con cantidades, rendimientos, personas y fechas) desde un Excel con **plantilla fija que controlamos nosotros**, con este flujo:

1. **Subir** el archivo → el sistema lo valida y lo guarda como **borrador** (draft). La estructura real de la obra no se toca.
2. **Revisar** el borrador: árbol, errores y avisos por fila, y el **diff** contra la estructura actual de la obra.
3. **Publicar** (aplicar a la plataforma en una única transacción atómica, todo o nada) o **descartar**.

Reglas rectoras: sin IA (parser determinista, columnas por encabezado normalizado); nunca se borra nada sin confirmación explícita y verificable; todo respeta el aislamiento por empresa y obra.

## Hechos confirmados en el código (observados, no validados en ejecución)

| # | Hecho | Fuente |
| --- | --- | --- |
| H1 | El backend es **Spring Boot 4.1.2** (Jackson 3, paquete `tools.jackson`), Java 17, Apache POI 5.5.1. El enunciado hablaba de Boot 3.2; el `pom.xml` indica 4.1.2 tras PRODUCTION-FOUNDATION-09. | `gestion_be/pom.xml`, `RubroCreateDto.java` |
| H2 | La estructura de actividades es `RubroObra` (tabla `rubros_obra`) con `parent`, `tipoNodo` (`RUBRO`/`NIVEL`/`SUBRUBRO`), `varianteEstructura` (solo raíz), `nRubro` (código jerárquico con puntos), `nombre`, `nivel`, `cantidad`, `unidad`, `rendimientoOficial/Ayudante`, `horasOficiales/AyudantesPrevistas`, `productividad`, `cantidadPersonas`, `frentes`, `fechaInicio/FinPlanificada`, `cronogramaManual`, `contrato`, `subcontratista`, `tipoContratista`, `empresa`. Profundidad máxima 3; solo la hoja es actividad. | `RubroObra.java`, `EstructuraRubros.java`, `VarianteEstructura.java` |
| H3 | Las variantes son `RUBRO_SUBRUBRO` (1 → 1.1 hoja), `RUBRO_NIVEL_SUBRUBRO` (1 → 1.1 nivel → 1.1.1 hoja subrubro) y `RUBRO_SUBRUBRO_NIVEL` (3 → 3.1 subrubro → 3.1.1 hoja nivel). **La plantilla descrita coincide con `RUBRO_SUBRUBRO_NIVEL`** (en 3 capas) o con `RUBRO_SUBRUBRO` (2 capas). En `RUBRO_SUBRUBRO_NIVEL` el servidor deriva `nivel` del **nombre** de la hoja (`nombreAncestroNivel`), por lo que el texto de la columna «Nivel» solo se conserva si pasa a ser el nombre de la hoja (ver P-1). | `EstructuraRubros.resolver` |
| H4 | La estructura **no se gestiona con un endpoint propio**: se persiste con `PUT /api/proyectos/{id}` (`ProyectoUpdateDto.rubros`), que **sincroniza la lista completa**: `UpdateProyectoUseCase.mapRubros` + `validateStructureChanges`, con bloqueo pesimista del proyecto (`bloquearPorId`). Las filas omitidas se eliminan, salvo las protegidas: no se elimina ni se cambia la unidad de una actividad con tareas vinculadas; no se elimina una vinculada a restricciones; no se elimina ni se cambia cantidad/unidad/empresa con avance en certificado cerrado (`CertificadoBloqueos`); no se cambia tipo/variante si hay tareas o restricciones. Los ids se conservan para las filas con `id`. | `UpdateProyectoUseCase.java`, `CertificadoBloqueos.java` |
| H5 | `RegistroUseCase` **no** gestiona estructura: es el alta de usuario y empresa. Las reglas de estructura viven en `EstructuraRubros` (validación del árbol), `RubroEstructura` (mapeo a `RubroObra`) y `RubroPlanning` (rendimientos ↔ HH, no negativos, finitos, ambos rendimientos o ninguno). `RubroPlanning`: con rendimientos y cantidad > 0 las HH previstas se **recalculan** como rendimiento × cantidad y `productividad` = oficial + ayudante; sin rendimientos pero con cantidad y HH, los rendimientos se **derivan**. | `RegistroUseCase.java`, `RubroPlanning.java` |
| H6 | Los nodos agrupadores no guardan datos de actividad (`limpiarAgrupador`); sus HH son la suma de sus hojas. Solo almacenan personas, frentes, fechas y `cronogramaManual` (`aplicarPlanificacionAgrupador`). `proyecto.horasPrevistas` se recalcula como suma de las hojas al actualizar. | `RubroEstructura.java`, `UpdateProyectoUseCase.java` |
| H7 | **No existe modelo de dependencias** entre actividades en backend ni frontend (búsqueda de «depend…/predecesor» sin resultados de dominio). «Depende de» no tiene destino de persistencia. | grep de ambos repos |
| H8 | El cálculo del **fin automático** vive en el frontend (`calculateLaborSchedule`, feriados argentinos locales, calendario laboral de la empresa, personas y frentes). El backend solo persiste fechas y la marca `cronogramaManual`. Según la propuesta FE 011 y `CronogramaSection.tsx`, las filas con `cronogramaManual=true` se presentan con «Ajuste manual» apagado y el fin automático como borrador pendiente, conservando el fin guardado hasta «Guardar». **No verificado en ejecución.** | `gestion_fe/lib/cronograma-laboral.ts`, `specs/011-cronograma-jerarquico/spec.md` |
| H9 | Producción usa **PostgreSQL + Flyway (V1–V21) con `ddl-auto: validate`**; el perfil local por defecto usa H2 con `ddl-auto: update`, `schema.sql` heredado (solo `ALTER`) y Flyway desactivado. Las migraciones se escriben en SQL portable H2/PostgreSQL. `gestion_be/AGENTS.md` dice que no se identificó un framework de migraciones; el repositorio sí lo tiene (Flyway); la discrepancia es de documentación. | `application*.yml`, `db/migration/` |
| H10 | `ImportController` (`POST /api/import/excel`, `@PreAuthorize("!hasRole('COLABORADOR')")`) delega en `ExcelImportService`: índices de columna fijos, hojas `RENDIMIENTOS` y `01-PRECIO DETALLADO-AVANCE DOC.`/`PRECIO DETALLADO`, crea `Proyecto` **sin `empresaRelacionada`**, `Entrega`, `Tarea` y `Rendimiento` (tablas del modelo antiguo), sin jerarquía `RubroObra`. Devuelve `e.getMessage()` al cliente en cualquier excepción. Un proyecto sin empresa es rechazado por `TenantAccessInterceptor` en `/api/proyectos/{id}/**` («La obra no tiene empresa asignada»); es decir, lo importado queda fuera del flujo multiempresa actual (inferido del código). Los fuentes de `ImportController`, `ExcelImportService`, `ProyectoController` y `GlobalExceptionHandler` llevan cabecera «Decompiled with CFR». | `ImportController.java`, `ExcelImportService.java`, `TenantAccessInterceptor.java` |
| H11 | El frontend tiene `/import` (`app/import/page.tsx`, `importApi.importExcel`), restringida a no colaboradores, con un aviso que admite no saber si sobrescribe datos. Hay un test que fija su alcance de invalidación de caché (`tests/context-queries.test.cjs`). | `gestion_fe/app/import/page.tsx`, `lib/api/administracion.ts` |
| H12 | Existe `ExcelParserSecurityTest`: valida que POI 5.5.1 rechaza entradas ZIP duplicadas en `XSSFWorkbook(InputStream)`. No hay límites propios de tamaño/filas/zip ni validación de MIME. `spring.servlet.multipart.max-file-size` es 50 MB, global. | `ExcelParserSecurityTest.java`, `application.yml` |
| H13 | Autorización: `TenantAccessInterceptor` aplica `requireEmpresa` a toda ruta `/api/proyectos/{id}/…`; `CurrentTenantService` permite a SUPER_ADMIN cualquier empresa. Roles: `ADMINISTRADOR`, `COLABORADOR`, `JEFE_DE_OBRA`, `RESPONSABLE`, `CONSULTA`. Las escrituras de certificados se restringen a ADMINISTRADOR y JEFE_DE_OBRA; `PUT /api/proyectos/{id}` **no** tiene `@PreAuthorize`. `docs/matriz-roles-permisos.md` deja «Importar proyecto desde Excel» sin decidir. | `TenantAccessInterceptor.java`, `CurrentTenantService.java`, `matriz-roles-permisos.md` |
| H14 | Errores HTTP: `GlobalExceptionHandler` mapea `SolicitudInvalidaException`→400, `ConflictoEstadoException`→409, `RecursoNoEncontradoException`→404, `AccessDeniedException`→403, `MaxUploadSizeExceededException`→413, conflictos de persistencia→409; `ErrorResponseDto(status, error, message)`; hay precedente de error con `code` (`FeatureUnavailableDto` con `FEATURE_TEMPORARILY_UNAVAILABLE`; `TEAM_PRO_REQUIRED` leído en el interceptor FE). El FE lee `response.data.message` (`apiErrorMessage`). | `GlobalExceptionHandler.java`, `lib/form.ts`, `lib/http-client.ts` |
| H15 | `unidades_metricas_empresa` es un catálogo por empresa (`codigo`, `nombre`), pero `RubroObra.unidad` es texto libre: hoy no se valida contra el catálogo. | `UnidadMetricaEmpresa.java`, `UpdateProyectoUseCase.java` |
| H16 | Los registros de horas enlazan con la actividad **por texto** (`registros_horas.numero_rubro` = código), y tareas, restricciones y certificados enlazan por `id` de `rubros_obra`. Por eso el **código** es la clave de correspondencia natural y los ids deben conservarse. | `V18__rubro_estructura_3_capas.sql`, `RegistroHorasRules.java` |
| H17 | El script de la spec 012 (`scripts/extract-empresas.py`) **aborta ante tablas no clasificadas** y su prueba `test_every_table_created_by_the_migrations_is_classified` falla si una migración crea una tabla que no figura en `OWNERSHIP`/`SPECIAL`/`EXCLUDED`/`KEEP_ALL`. | `scripts/extract-empresas.py`, `scripts/tests/test_extract_empresas.py` |
| H18 | Las funcionalidades con interruptor usan propiedades `app.features.*` leídas por `ProductionFeaturePolicy` (p. ej. `app.features.gastos-fijos-enabled`) y responden 503 con `FeatureUnavailableException`. | `ProductionFeaturePolicy.java`, `application.yml` |

**No verificado:** no se abrieron `PREV VS REAL.xlsx` ni `VÓRTISE OS (1).pdf` de la raíz del workspace; el formato de la plantilla se toma exclusivamente del enunciado aprobado. Ver P-16.

## Alcance

- Plantilla fija v1, descargable desde la plataforma, y su parser determinista.
- Borrador persistente por obra (un borrador activo por obra), revisión (árbol, incidencias, diff) y descarte.
- Publicación atómica con dos modos: **AGREGAR** y **REEMPLAZAR** (sincronización por código, conserva ids).
- Contrato API nuevo bajo `/api/proyectos/{proyectoId}/importaciones-estructura` (propiedad de `gestion_be`) y pantalla en `gestion_fe`.
- Migración Flyway aditiva y entidad JPA del borrador.
- Auditoría mínima (metadatos del borrador y logs estructurados sin contenido de celdas).

## No alcance

- Importar cualquier otro dato: tareas, avances, horas, restricciones, contratos, subcontratistas, presupuesto, costos, plantas. (La plantilla no los trae.)
- Crear la obra desde el Excel: la obra debe existir. (El importador antiguo sí crea proyectos; este no.)
- IA, adivinar columnas, plantillas libres o formatos distintos de la plantilla v1.
- Persistir las **dependencias** (DC-03), calcular fechas desde dependencias o duración (DC-02) o implementar un motor de planificación en backend.
- Importar totales/subtotales (DC-01) y asignar contratista/empresa/contrato por actividad (la plantilla no tiene esas columnas; se **preservan** en actividades existentes).
- Formatos `.xls`, `.xlsm`, `.csv`, `.ods`; archivos con macros, vínculos externos u objetos incrustados.
- Deshacer una publicación (no hay «revertir»; ver Riesgos y Reversión en el plan).
- Modificar, deprecar formalmente o eliminar `/api/import/excel` y `/import` (ver «Compatibilidad con el importador existente»).

## Plantilla v1 (formato fijo)

Hoja `Estructura` (primera hoja visible). Filas 1–3: título, instrucciones y «Versión de plantilla: 1» (informativas). **Encabezados en la fila 4**. Datos desde la fila 5.

| Columna (encabezado) | Clave normalizada | Requerida | Destino (`RubroObra` / borrador) | Tratamiento |
| --- | --- | --- | --- | --- |
| Código | `codigo` | Sí | `nRubro` | Texto jerárquico `3`, `3.1`, `3.1.1` |
| Rubro / Subrubro | `rubro subrubro` | Sí | `nombre` | Nombre del nodo (≤ 255) |
| Nivel | `nivel` | Sí | `nivel` (y `nombre` de la hoja en 3 capas, P-1) | Solo hojas |
| Cantidad | `cantidad` | Sí | `cantidad` | Solo hojas |
| Unidad | `unidad` | Sí | `unidad` | Solo hojas |
| Tipo de APU | `tipo de apu` | No | — | Informativo; no se guarda (P-3) |
| APU Oficial (HH/u) | `apu oficial hh u` | No | `rendimientoOficial` | Solo hojas |
| APU Ayudante (HH/u) | `apu ayudante hh u` | No | `rendimientoAyudante` | Solo hojas |
| HH totales Oficial | `hh totales oficial` | No | `horasOficialesPrevistas` | **Recalculado** (DC-01); se contrasta |
| HH totales Ayudante | `hh totales ayudante` | No | `horasAyudantesPrevistas` | **Recalculado**; se contrasta |
| HH totales | `hh totales` | No | — | Solo contraste |
| Productividad (HH/u) | `productividad hh u` | No | `productividad` | Recalculada = oficial + ayudante; se contrasta |
| Personas | `personas` | No | `cantidadPersonas` | Entero ≥ 1 |
| Duración (días) | `duracion dias` | No | — | Informativa; no se guarda (DC-02) |
| Depende de | `depende de` | No | — | Se valida y se muestra; **no se guarda** (DC-03) |
| Inicio previsto | `inicio previsto` | No | `fechaInicioPlanificada` | dd/MM/yyyy |
| Fin previsto | `fin previsto` | No | `fechaFinPlanificada` | dd/MM/yyyy |

Normalización de encabezados (determinista, sin IA): recortar, pasar a minúsculas, quitar tildes (NFD), reemplazar todo carácter no alfanumérico por un espacio y colapsar espacios. El parser **localiza la fila de encabezados** buscando en las filas 1–10 la que contiene `codigo` y `rubro subrubro`; mapea columnas por clave y no por posición. Los valores del enunciado («fila 4») son el caso esperado, no una restricción rígida.

### Interpretación de la jerarquía

- Profundidad 1 (`3`): **RUBRO**, raíz. Profundidad 2 (`3.1`): **SUBRUBRO** agrupador (o hoja si el rubro es de 2 capas). Profundidad 3 (`3.1.1`): **NIVEL** hoja.
- **Variante por rubro raíz** (inferida, determinista): si el rubro tiene algún descendiente de profundidad 3 → `RUBRO_SUBRUBRO_NIVEL`; si no → `RUBRO_SUBRUBRO`. `RUBRO_NIVEL_SUBRUBRO` **no se produce** con la plantilla v1 (P-2). Pueden coexistir rubros de 2 y 3 capas en el mismo archivo.
- Se envía a la validación autoritativa (`EstructuraRubros.resolver`) con `tipoNodo`, `parentCodigo` y `varianteEstructura` explícitos, igual que el cliente web, de modo que **las reglas del flujo manual y las del importador son las mismas** (profundidad, tipo por capa, código consistente con el padre, unicidad).
- Hoja (según `EstructuraRubros.esHoja`): cuelga de un rubro y está en la última capa de su variante. Solo la hoja lleva cantidad, unidad, rendimientos, HH, personas y fechas.
- Agrupadores (rubro y subrubro con hijos): se importan **solo `código`, `nombre` y tipo**. Cantidad, APU, HH, personas, fechas y totales de esas filas se ignoran (aviso `DATOS_EN_AGRUPADOR_IGNORADOS`) y, si el nodo ya existe, **se preservan** sus datos actuales (H6).

## Reglas de parseo y validación

Severidades: **ERROR** (bloquea la publicación), **AVISO** (no bloquea; se muestra), **INFO**. Cada incidencia lleva `codigo` estable, `severidad`, `fila`, `columna`, `celda` (p. ej. `E12`), `codigoNodo` y `mensaje` en español. Una incidencia nunca contiene el contenido íntegro de una celda de texto libre (máx. 80 caracteres, escapado).

### Rechazos de archivo (sin borrador; HTTP 400/413)

`ARCHIVO_VACIO`, `ARCHIVO_EXTENSION_INVALIDA` (solo `.xlsx`), `ARCHIVO_FIRMA_INVALIDA` (no empieza por `PK\x03\x04`), `ARCHIVO_DEMASIADO_GRANDE` (413; > 2 MiB por defecto), `ARCHIVO_ZIP_INSEGURO` (ver Seguridad), `ARCHIVO_ENLACE_EXTERNO` (existe `xl/externalLinks/`), `ARCHIVO_MACROS` (`vbaProject.bin` o tipo de contenido con macros), `ARCHIVO_ILEGIBLE`, `PLANTILLA_SIN_CABECERA`, `PLANTILLA_COLUMNA_REQUERIDA_AUSENTE`, `PLANTILLA_CABECERA_DUPLICADA`, `PLANTILLA_VERSION_INCOMPATIBLE` (versión mayor distinta), `PLANTILLA_SIN_FILAS`, `PLANTILLA_DEMASIADAS_FILAS` (> 2.000 filas de datos por defecto).

### Filas y celdas (se guardan en el borrador)

| Regla | Código | Severidad |
| --- | --- | --- |
| Filas completamente vacías se omiten | — | — |
| Código vacío en fila con datos | `CODIGO_OBLIGATORIO` | ERROR |
| Código no cumple `[1-9]\d{0,8}(\.[1-9]\d{0,8}){0,2}` (sin ceros a la izquierda; coincide con `CODIGO_ESTRICTO`) | `CODIGO_FORMATO_INVALIDO` | ERROR |
| Código con más de 3 segmentos | `PROFUNDIDAD_EXCEDIDA` | ERROR |
| Código de celda **numérica no entera** (Excel convierte «3.10» en 3,1; «3.1» y «3.10» serían indistinguibles). Un numérico entero (`3`) se acepta como «3». La plantilla formatea la columna como texto | `CODIGO_NUMERICO_AMBIGUO` | ERROR |
| Código repetido (se marca **cada** aparición, citando las filas) | `CODIGO_DUPLICADO` | ERROR |
| Código con padre ausente (`3.1.1` sin `3.1`; `3.1` sin `3`). No se generan padres implícitos | `PADRE_INEXISTENTE` | ERROR |
| Nombre vacío / > 255 | `NOMBRE_OBLIGATORIO` / `NOMBRE_DEMASIADO_LARGO` | ERROR |
| Número ilegible (texto no numérico, `#DIV/0!`, `#REF!`, etc.) | `NUMERO_INVALIDO` / `CELDA_CON_ERROR` | ERROR |
| Número negativo en cantidad, APU, HH, productividad, duración | `VALOR_NEGATIVO` | ERROR |
| No finito o > 1e9 (evita desbordes en cantidad × rendimiento) | `VALOR_FUERA_DE_RANGO` | ERROR |
| Cantidad con unidad vacía | `UNIDAD_REQUERIDA` | ERROR |
| Cantidad vacía en hoja | `CANTIDAD_AUSENTE` | AVISO |
| Cantidad = 0 | `CANTIDAD_CERO` | AVISO |
| Solo uno de APU Oficial / Ayudante (`RubroPlanning` exige ambos o ninguno) | `APU_INCOMPLETO` | ERROR |
| Personas no entera o < 1 | `PERSONAS_INVALIDAS` | ERROR |
| Duración no entera o < 0 | `DURACION_INVALIDA` | ERROR |
| Fecha ilegible, inexistente (31/02) o fuera de 01/01/2000–31/12/2100 | `FECHA_INVALIDA` / `FECHA_FUERA_DE_RANGO` | ERROR |
| Fin < inicio (misma regla que `RubroPlanning`) | `FECHAS_INVERTIDAS` | ERROR |
| Inicio sin fin / fin sin inicio | `FECHA_FIN_AUSENTE` / `FECHA_INICIO_AUSENTE` | AVISO |
| Unidad que no está en el catálogo de la empresa (comparación sin mayúsculas/tildes; si coincide se **normaliza al código del catálogo**) | `UNIDAD_NO_CATALOGADA` | AVISO (P-10) |
| HH totales o productividad del archivo difieren de lo recalculado (> max(0,01; 0,5 %)) | `HH_TOTAL_DISCREPA` / `PRODUCTIVIDAD_DISCREPA` | AVISO |
| Totales de rubro/subrubro difieren de la suma recalculada (misma tolerancia) | `TOTAL_DISCREPA` | AVISO |
| Datos de actividad en un agrupador | `DATOS_EN_AGRUPADOR_IGNORADOS` | AVISO |
| Rubro sin actividades / subrubro de 3 capas sin hijos y sin cantidad | `RUBRO_SIN_ACTIVIDADES` | AVISO |
| Subrubro de 3 capas sin hijos **con** cantidad (no sería actividad) | `ESTRUCTURA_INVALIDA` | ERROR |
| «Rubro / Subrubro» y «Nivel» de una hoja difieren en 3 capas (P-1) | `NOMBRE_HOJA_DISTINTO` | AVISO |
| Tipo de APU presente | `TIPO_APU_NO_APLICADO` | INFO (una vez) |
| Columna fuera de la plantilla / opcional ausente | `COLUMNA_IGNORADA` / `COLUMNA_OPCIONAL_AUSENTE` | AVISO |
| Fila oculta con datos / celdas combinadas en el área de datos | `FILA_OCULTA` / `CELDAS_COMBINADAS` | AVISO |
| Falla de la validación autoritativa no prevista antes (red de seguridad) | `ESTRUCTURA_INVALIDA` | ERROR |

### Números, fechas, fórmulas

- **Números**: celdas numéricas se usan tal cual (sin localización). En celdas de **texto** se acepta la convención es-AR: coma decimal y punto de miles únicamente en grupos de 3 (`1.234,56`, `12,5`, `1234`). Un punto con exactamente tres dígitos detrás y sin coma (`1.234`) es ambiguo → `NUMERO_AMBIGUO` (ERROR); un punto con ≠ 3 dígitos se acepta como decimal (`12.5`). Sin `Locale` por defecto del servidor; sin notación científica ni `NaN`/`Infinity`.
- **Fechas**: celda de fecha de Excel (respetando el sistema 1900/1904 de POI) o texto estricto `dd/MM/yyyy` (`\d{1,2}/\d{1,2}/\d{4}`, calendario válido). Se descarta la hora. Ningún otro formato de texto se acepta.
- **Fórmulas**: se lee **solo el valor en caché** (`getCachedFormulaResultType`); **no se evalúan** (ni `FormulaEvaluator`, ni funciones externas, ni libros vinculados). Fórmula sin valor en caché → `FORMULA_SIN_VALOR` (ERROR: «abrí y guardá el archivo en Excel»). Fórmula con resultado de error → `CELDA_CON_ERROR`.
- Se limita el texto de cada celda a 1.000 caracteres (el resto se trunca con aviso) y se consideran como máximo 30 columnas.

### Dependencias («Depende de»)

Gramática: lista separada por `,` o `;`. Cada elemento: `INICIO` | `CODIGO` | `CODIGO (±N d)`, con `INICIO` = «Inicio de obra» (sin mayúsculas/tildes), `CODIGO` como arriba y `N` entero 0–3650 con sufijo `d`/`día`/`dias`. Ejemplos válidos: `3.1.2, 5.1, 5.2`; `3.2.1 (+10 d)`; `Inicio de obra`.

| Regla | Código | Severidad |
| --- | --- | --- |
| Elemento que no cumple la gramática | `DEPENDENCIA_SINTAXIS` | ERROR |
| Código que no existe en el archivo | `DEPENDENCIA_NO_RESUELTA` | ERROR |
| La actividad depende de sí misma | `DEPENDENCIA_PROPIA` | ERROR |
| Ciclo en el grafo de dependencias (se informa el camino, máx. 10 códigos) | `DEPENDENCIA_CICLO` | ERROR |
| Dependencia de un agrupador (se interpreta como todas sus hojas descendientes) | `DEPENDENCIA_A_AGRUPADOR` | AVISO |
| «Depende de» en un agrupador (se ignora) | `DATOS_EN_AGRUPADOR_IGNORADOS` | AVISO |
| Inicio < fin de la predecesora + desfase (días corridos; fin-a-inicio) | `INICIO_ANTES_DE_PREDECESORA` | AVISO |
| `Inicio de obra` y el inicio de la actividad es anterior a `proyecto.fechaInicio` | `INICIO_ANTES_DE_OBRA` | AVISO |
| `Inicio de obra` y la obra no tiene `fechaInicio` | `INICIO_DE_OBRA_SIN_FECHA_OBRA` | AVISO |
| Existen dependencias en el archivo | `DEPENDENCIAS_NO_PERSISTIDAS` | INFO (una vez) |

«Inicio de obra» no es un nodo: es un ancla y no participa del grafo de ciclos. La detección de ciclos se hace sobre las hojas (expandiendo dependencias de agrupadores). Si P-7 cambia la severidad de referencias sin resolver o ciclos a AVISO, solo cambia la tabla.

## Decisiones de comportamiento (requieren aprobación)

### DC-01 — Totales y subtotales: **recalcular, no importar** (recomendada)

| Opción | Pros | Contras |
| --- | --- | --- |
| **A. Recalcular siempre** (recomendada) | Coherente con `RubroPlanning` (HH = APU × cantidad) y con H6 (los agrupadores no guardan HH); sin fuentes de verdad duplicadas; `proyecto.horasPrevistas` queda consistente | Si el Excel trae un total «a mano» distinto, se descarta (se avisa) |
| B. Importar los totales | Reproduce el Excel | El modelo no tiene dónde guardarlos en agrupadores; HH de hoja podrían contradecir cantidad × APU |

Con A: la hoja persiste `rendimientoOficial/Ayudante` y `RubroPlanning` deriva HH y productividad. Si faltan APU pero hay HH totales y cantidad > 0, los rendimientos se **derivan** (`RubroPlanning`). Los totales del archivo solo generan avisos de discrepancia. Tras publicar, `proyecto.horasPrevistas` se recalcula como en la actualización manual.

### DC-02 — Cronograma: fechas importadas vs. recalculadas (**decisión explícita**)

Hechos: el backend no calcula fechas (H8); el fin automático depende de calendario laboral, feriados, personas y frentes en el FE; `cronogramaManual` indica que el fin guardado no se recalcula; no hay dependencias persistidas (H7).

| Opción | Descripción | Pros | Contras |
| --- | --- | --- | --- |
| **A. Importar fechas tal cual; `cronogramaManual = true` si hay inicio y fin** (recomendada) | El plan del Excel (ya consistente con sus dependencias) es la fuente; sin inicio/fin completos → se importa lo presente, `cronogramaManual=false` | Fiel al archivo; sin lógica nueva en BE; ediciones posteriores de HH/personas no mueven el fin hasta que el usuario lo decida | La UI actual presenta filas manuales con el fin automático como borrador pendiente (H8): debe verificarse que las barras muestren el rango importado (criterio AC-22) |
| B. Solo inicio; el fin lo calcula el FE | Se importa `fechaInicioPlanificada`; el fin se deja vacío y lo calcula la UI de Cronograma | Usa el calendario laboral y los feriados de la empresa | Pierde el fin del archivo; el fin solo se persiste cuando el usuario guarda en Cronograma; requiere personas y HH |
| C. Recalcular todo desde duración + dependencias | El servidor calcula inicio y fin | Plan «vivo» coherente con dependencias | Exige portar calendario/feriados a BE y persistir dependencias; fuera de alcance; riesgo alto |
| D. Importar fechas con `cronogramaManual = false` | Fechas del archivo, fin tratado como automático | Sin marca manual | La primera edición recalcula el fin y sobrescribe el del archivo |

**Recomendación: A.** Detalles: «Duración (días)» no se guarda; los agrupadores no reciben fechas del archivo (se preservan las existentes); en REEMPLAZAR, las fechas del archivo **ganan** sobre las existentes y el diff las muestra campo a campo. No hay recálculo en el servidor. Requiere aprobación (P-6) y una verificación manual del Cronograma antes de aprobar el cierre.

### DC-03 — Dependencias: **validar y mostrar, no persistir** (recomendada)

Sin modelo ni UI para dependencias, persistirlas crearía datos que nada consume. En v1 se validan (referencias, ciclos, coherencia de fechas), se muestran en la revisión y se conservan en el borrador; **al publicar se pierden** y la UI lo advierte. Alternativa: tabla `rubro_dependencias` + UI → spec posterior (P-7).

### DC-04 — Publicar sin errores; avisos no bloquean

Un borrador con ≥ 1 ERROR no es publicable (`publicable=false`; publicar devuelve 409 `BORRADOR_CON_ERRORES`). Los avisos se muestran y no bloquean; la pantalla no los oculta. El backend no exige acuse de avisos.

## Diff y modos de publicación

**Correspondencia por código** (`nRubro`, único por obra; H16). Se compara el borrador contra la estructura actual de la obra, **recalculado cada vez** que se consulta o publica.

Campos comparados: `tipoNodo`, `nombre`, `parentCodigo`, `varianteEstructura` (raíz), y en hojas `nivel`, `cantidad`, `unidad`, `rendimientoOficial`, `rendimientoAyudante`, `cantidadPersonas`, `fechaInicioPlanificada`, `fechaFinPlanificada`. Números con tolerancia 1e-6 relativa; textos normalizados (trim, mayúsculas/tildes solo para comparar).

| Clase | Definición |
| --- | --- |
| `AGREGADO` (A) | Código del borrador que no existe en la obra |
| `SIN_CAMBIOS` (S) | Mismo código y mismos campos comparados |
| `MODIFICADO` (M) | Mismo código, algún campo distinto. **Estructural** (Me) si cambia `tipoNodo`, `nombre`, `parentCodigo` o `varianteEstructura`; si no, de datos (Md) |
| `ELIMINADO` (R) | Código de la obra que no está en el borrador |

N_e = nodos actuales de la obra (todos los tipos). **Cambio estructural** = (R + Me) / N_e.

### Clasificación y modos (umbral objetivo)

Umbral configurable `app.importacion-estructura.umbral-cambio-estructural`, **por defecto 0,30** (30 %; P-4).

| Clasificación | Condición | Modos permitidos | Confirmación requerida |
| --- | --- | --- | --- |
| `SIN_ESTRUCTURA_PREVIA` | N_e = 0 | `AGREGAR` | Pantalla muestra qué se agregará; botón «Publicar» |
| `SIN_CAMBIOS` | A = M = R = 0 | ninguno | No hay nada que publicar |
| `SOLO_AGREGA` | R = 0, M = 0, A > 0 | `AGREGAR` | Lista de lo que se agregará + confirmar |
| `CAMBIOS_MENORES` | (R + M) > 0 y cambio estructural < umbral | `AGREGAR` (solo A; ignora M y R) o `REEMPLAZAR` | `REEMPLAZAR` con R > 0 exige `confirmaciones.eliminaciones == R` |
| `MUY_DIFERENTE` | cambio estructural ≥ umbral (incluye códigos sin ninguna coincidencia) | **solo `REEMPLAZAR`** | `confirmaciones.eliminaciones == R` **y** `confirmaciones.textoConfirmacion == "REEMPLAZAR"`; la UI pregunta explícitamente «¿Reemplazar la estructura actual?» mostrando cuántos nodos desaparecen |

Semántica de los modos:

- **AGREGAR**: inserta únicamente los `AGREGADO`. No modifica ni elimina nada. Requiere que cada nodo agregado tenga padre en (existentes ∪ agregados) y que el árbol resultante supere `EstructuraRubros.resolver` (si agregar un nodo de profundidad 3 bajo un rubro existente de 2 capas rompe la variante → bloqueo `AGREGAR_INCOMPATIBLE`).
- **REEMPLAZAR**: **sincroniza por código**: actualiza los `MODIFICADO` (conserva `id`), inserta los `AGREGADO` y elimina los `ELIMINADO`. **No** es «borrar todo y recrear»: los ids se conservan para que tareas, restricciones, certificados y horas sigan enlazados (H16). Los campos fuera de plantilla (contratista, empresa, contrato, frentes, `subNumeroRubro`) se **preservan** en los nodos existentes.
- **Celdas opcionales vacías no borran**: en `REEMPLAZAR`, una celda vacía de una columna opcional (APU, personas, fechas) significa «sin dato en el archivo» y **conserva** el valor existente; el diff compara solo los campos con valor en el archivo. Borrar un valor existente no es posible desde la plantilla v1 (evita pérdidas sin confirmación). Si el archivo trae inicio y fin, `cronogramaManual` pasa a `true` (DC-02); en otro caso se conserva el existente.
- **Eliminaciones**: nunca se eliminan sin `modo=REEMPLAZAR` y la confirmación numérica exacta (el servidor compara con su propio R recalculado dentro de la transacción).

### Bloqueos (impiden publicar, con detalle por código)

`NODO_CON_DATOS_ASOCIADOS`: un `ELIMINADO` (o un cambio de tipo/variante) afecta a un nodo con tareas, restricciones vinculadas, avance en certificado cerrado o completado en certificado, o con registros de horas por `numero_rubro`. También se reportan: cambio de unidad con tareas vinculadas; cambio de cantidad/unidad/empresa con avance en certificado cerrado; `AGREGAR_INCOMPATIBLE`. Los bloqueos reutilizan las **mismas reglas del flujo manual** (H4) y la publicación aborta completa si hay alguno (nunca publicación parcial). El usuario puede corregir el Excel o descartar.

## Ciclo de vida del borrador

Estados: `BORRADOR`, `PUBLICADO`, `DESCARTADO`, `EXPIRADO`.

| Transición | Disparador | Efecto |
| --- | --- | --- |
| (nuevo) → `BORRADOR` | `POST` de archivo con estructura de plantilla reconocible | Un único `BORRADOR` por obra (puede traer errores de fila: se revisa pero no se publica) |
| `BORRADOR` → `PUBLICADO` | `POST …/publicar` correcto | Misma transacción que los cambios; guarda usuario, fecha, modo y conteos; se anula el payload |
| `BORRADOR` → `DESCARTADO` | `DELETE …/{id}` o nueva subida con `?reemplazarBorrador=true` | Se anula el payload; idempotente |
| `BORRADOR` → `EXPIRADO` | Al acceder, si `ahora > expiraEn` (**evaluación perezosa**, sin tareas programadas: no existen en el código) | No publicable; se anula el payload |

- **Un borrador activo por obra**: garantizado por la base (índice único); una segunda subida concurrente recibe 409 `BORRADOR_ACTIVO` con el id existente.
- **Expiración**: 7 días por defecto (`app.importacion-estructura.draft-ttl-dias`, P-9), contados desde la subida.
- **Concurrencia**: `version` optimista (`@Version`); publicar bloquea la obra con `bloquearPorId` (el mismo bloqueo que `UpdateProyectoUseCase` y `RegistroHorasService`), vuelve a validar estado/versión/expiración, **recalcula el diff y exige que su huella coincida con la que vio el usuario** (`huellaDiff`); si no, 409 `DIFF_DESACTUALIZADO`. Una edición manual de la estructura entre revisión y publicación cambia la huella.
- El borrador **no modifica** `rubros_obra` ni ninguna otra tabla de negocio. El archivo `.xlsx` original **no se guarda**; se guardan solo el contenido parseado, el SHA-256 y el nombre saneado.
- Eliminar una obra elimina sus borradores (FK con `ON DELETE CASCADE`).
- Cualquier usuario autorizado de la obra puede ver, publicar o descartar el borrador activo, no solo quien lo subió (P-12).

## Contrato API

Base: `/api/proyectos/{proyectoId}/importaciones-estructura`. La ruta cuelga de `/api/proyectos/{id}` para que `TenantAccessInterceptor` verifique empresa/obra **antes** del controlador. Autorización: ver Seguridad. Fechas/horas en ISO-8601 sin zona (`2026-11-17T17:40:00`), `LocalDate` como `yyyy-MM-dd` (formato de serialización por confirmar contra los DTO existentes al implementar). Los números nulos viajan como `null`, no se omiten.

| Método y ruta | Propósito | Éxito | Errores principales |
| --- | --- | --- | --- |
| `GET …/plantilla` | Descarga `plantilla-estructura-obra.xlsx` (generada) | 200 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | 401, 403, 404 |
| `POST …` (`multipart/form-data`: `file`; query `reemplazarBorrador=false`) | **Subir y crear borrador** (equivale a «preview»: valida, parsea, guarda) | 201 `ImportacionEstructuraResumenDto` | 400 `ARCHIVO_*`/`PLANTILLA_*`, 413, 409 `BORRADOR_ACTIVO`, 403 |
| `GET …/activo` | Resumen del borrador `BORRADOR` vigente | 200 `ImportacionEstructuraResumenDto` / 204 si no hay | 403 |
| `GET …/{id}` | Detalle: nodos normalizados e incidencias | 200 `ImportacionEstructuraDetalleDto` | 404 (no existe o es de otra obra), 409 `BORRADOR_EXPIRADO` |
| `GET …/{id}/diff` | Diff contra la estructura actual (recalculado) | 200 `ImportacionEstructuraDiffDto` | 404, 409 `BORRADOR_NO_VIGENTE` |
| `POST …/{id}/publicar` | Aplicar (atómico) | 200 `ImportacionEstructuraPublicacionDto` | 400 `CONFIRMACION_REQUERIDA`, 409 (ver abajo), 404, 403 |
| `DELETE …/{id}` | **Descartar** | 204 (idempotente en `DESCARTADO`) | 404, 409 si ya `PUBLICADO` |

### `ImportacionEstructuraResumenDto`

```json
{
  "id": 15,
  "proyectoId": 42,
  "estado": "BORRADOR",
  "version": 0,
  "archivoNombre": "estructura-obra.xlsx",
  "archivoBytes": 48213,
  "archivoSha256": "9f2c…e1a7",
  "plantillaVersion": "1",
  "creadoEn": "2026-11-17T17:40:00",
  "creadoPorUsuarioId": 7,
  "expiraEn": "2026-11-24T17:40:00",
  "totales": { "nodos": 143, "rubros": 12, "agrupadores": 31, "actividades": 100, "errores": 2, "avisos": 5 },
  "publicable": false
}
```

### `ImportacionEstructuraDetalleDto`

```json
{
  "resumen": { "...": "ImportacionEstructuraResumenDto" },
  "nodos": [
    {
      "fila": 7, "codigo": "3.1.1", "parentCodigo": "3.1", "profundidad": 3,
      "tipoNodo": "NIVEL", "hoja": true, "varianteEstructura": null,
      "nombre": "Subsuelo", "nivel": "SS1",
      "cantidad": 120.5, "unidad": "M3",
      "tipoApu": "Estándar",
      "apuOficial": 1.2, "apuAyudante": 2.4,
      "hhOficial": 144.6, "hhAyudante": 289.2, "hhTotal": 433.8, "productividad": 3.6,
      "hhOficialArchivo": 144.6, "hhAyudanteArchivo": 289.2,
      "personas": 4, "duracionDias": 12,
      "dependencias": [
        { "tipo": "ACTIVIDAD", "codigo": "3.1.2", "desfaseDias": 0, "resuelta": true },
        { "tipo": "ACTIVIDAD", "codigo": "3.2.1", "desfaseDias": 10, "resuelta": true },
        { "tipo": "INICIO_OBRA", "codigo": null, "desfaseDias": 0, "resuelta": true }
      ],
      "dependeDeTexto": "3.1.2, 3.2.1 (+10 d), Inicio de obra",
      "fechaInicio": "2026-12-01", "fechaFin": "2026-12-16",
      "incidencias": [
        { "severidad": "AVISO", "codigo": "UNIDAD_NO_CATALOGADA", "columna": "Unidad", "celda": "E7", "mensaje": "La unidad «M3» no está en el catálogo de la empresa." }
      ]
    }
  ],
  "incidencias": [
    { "severidad": "ERROR", "codigo": "CODIGO_DUPLICADO", "fila": 12, "columna": "Código", "celda": "A12", "codigoNodo": "3.1.4", "mensaje": "El código 3.1.4 también aparece en la fila 9." }
  ]
}
```

`hhOficial/hhAyudante/hhTotal/productividad` son los valores **recalculados** que se publicarían (DC-01); `…Archivo` son los leídos. En agrupadores, los campos de actividad vienen en `null`. `incidencias` del nivel raíz incluye **todas** (también las de nodo).

### `ImportacionEstructuraDiffDto`

```json
{
  "importacionId": 15,
  "versionBorrador": 0,
  "huellaDiff": "sha256:4be0…c912",
  "calculadoEn": "2026-11-17T17:45:10",
  "estructuraActual": { "nodos": 120, "actividades": 88 },
  "conteos": { "agregados": 14, "sinCambios": 100, "modificados": 4, "modificadosEstructurales": 1, "eliminados": 2 },
  "cambioEstructural": 0.025,
  "umbral": 0.30,
  "clasificacion": "CAMBIOS_MENORES",
  "modosPermitidos": ["AGREGAR", "REEMPLAZAR"],
  "requiereTextoConfirmacion": false,
  "eliminacionesAConfirmar": 2,
  "bloqueos": [
    { "codigo": "NODO_CON_DATOS_ASOCIADOS", "nodo": "2.1.3", "detalle": "La actividad tiene 3 tareas vinculadas." }
  ],
  "publicable": false,
  "cambios": [
    { "tipo": "AGREGADO", "codigo": "4.2.1", "nombre": "Contrapiso", "parentCodigo": "4.2", "campos": [] },
    { "tipo": "MODIFICADO", "codigo": "3.1.1", "estructural": false, "conDatosAsociados": true,
      "campos": [ { "campo": "cantidad", "antes": 100.0, "despues": 120.5 } ] },
    { "tipo": "ELIMINADO", "codigo": "5.9", "nombre": "Varios", "conDatosAsociados": true, "campos": [] }
  ]
}
```

`cambios` omite `SIN_CAMBIOS`. `publicable` combina «sin errores», «sin bloqueos» y «al menos un modo permitido».

### Publicar — petición y respuesta

```json
{
  "modo": "REEMPLAZAR",
  "huellaDiff": "sha256:4be0…c912",
  "versionBorrador": 0,
  "confirmaciones": { "eliminaciones": 2, "textoConfirmacion": "REEMPLAZAR" }
}
```

```json
{
  "importacionId": 15,
  "estado": "PUBLICADO",
  "modo": "REEMPLAZAR",
  "agregados": 14,
  "actualizados": 4,
  "eliminados": 2,
  "sinCambios": 100,
  "horasPrevistasObra": 12345.6,
  "publicadoEn": "2026-11-17T17:50:02"
}
```

`textoConfirmacion` solo es obligatorio en `MUY_DIFERENTE`; `confirmaciones.eliminaciones` es obligatorio cuando R > 0 y `modo=REEMPLAZAR` (en `AGREGAR` no aplica y debe omitirse).

### Errores

Cuerpo: `{ "status": 409, "error": "Conflict", "code": "DIFF_DESACTUALIZADO", "message": "…", "detalle": [ … ] }` (compatible con `ErrorResponseDto` más `code`; `detalle` solo en bloqueos/errores de borrador, sin datos de otras obras).

| HTTP | `code` | Cuándo |
| --- | --- | --- |
| 400 | `ARCHIVO_*`, `PLANTILLA_*` | Rechazos de archivo (sección de parseo) |
| 400 | `CONFIRMACION_REQUERIDA` | Falta o no coincide `eliminaciones`/`textoConfirmacion`; modo no permitido |
| 403 | — | Rol no autorizado u obra de otra empresa (mensaje genérico, sin detalle ajeno) |
| 404 | — | Obra o borrador inexistente **o de otra obra** (mismo mensaje) |
| 409 | `BORRADOR_ACTIVO` | Ya existe un borrador activo en la obra (incluye su `id`) |
| 409 | `BORRADOR_NO_VIGENTE` / `BORRADOR_EXPIRADO` | Estado distinto de `BORRADOR` / vencido |
| 409 | `VERSION_DESACTUALIZADA` | `versionBorrador` no coincide |
| 409 | `DIFF_DESACTUALIZADO` | La huella cambió: la obra se modificó tras la revisión |
| 409 | `BORRADOR_CON_ERRORES` | Hay ERROR en el borrador |
| 409 | `BLOQUEOS_DE_PUBLICACION` | Bloqueos (`detalle` lista códigos de nodo) |
| 413 | `ARCHIVO_DEMASIADO_GRANDE` | Excede el máximo |
| 503 | `FEATURE_TEMPORARILY_UNAVAILABLE` | Funcionalidad desactivada por configuración (patrón existente, H18) |
| 500 | — | Mensaje genérico; sin trazas ni contenido del archivo |

## Compatibilidad DTO backend ↔ tipos TypeScript

Propietario del contrato: `gestion_be`. El FE define tipos espejo en `gestion_fe/types/index.ts` y un módulo `importacionEstructuraApi` en `lib/api/obras.ts`.

| Java (record, `application/dto`) | TypeScript (`types/index.ts`) | Notas |
| --- | --- | --- |
| `ImportacionEstructuraResumenDto` | `ImportacionEstructuraResumenDto` | `estado`: `"BORRADOR" \| "PUBLICADO" \| "DESCARTADO" \| "EXPIRADO"` |
| `ImportacionEstructuraDetalleDto`, `NodoBorradorDto`, `DependenciaDto`, `IncidenciaDto` | idem | `tipoNodo` reutiliza `TipoNodoRubro`; `varianteEstructura` reutiliza `VarianteEstructura` |
| `ImportacionEstructuraDiffDto`, `CambioNodoDto`, `CampoCambiadoDto`, `BloqueoDto` | idem | `antes`/`despues`: `string \| number \| boolean \| null` |
| `ImportacionEstructuraPublicarRequest`, `…PublicacionDto` | idem | `modo`: `"AGREGAR" \| "REEMPLAZAR"` |
| `ImportacionErrorDto` | `{ status; error; code?: string; message: string; detalle?: unknown[] }` | `apiErrorMessage` sigue leyendo `message` |

- `Double`→`number | null`; `Long`→`number`; `LocalDate`/`LocalDateTime`→`string`; enums→uniones de cadenas literales (se añaden valores solo de forma compatible: el FE trata un valor desconocido como un caso por defecto visible, nunca como éxito).
- Los tipos nuevos usan `T | null` para campos que el backend envía como `null` (los tipos antiguos usan `?:`; no se mezclan en los nuevos).
- El FE **no reutiliza** `RubroDto` para el borrador (tiene `nRubro`, ids y campos de contratos que el borrador no tiene); adapta los nodos a `NodoEstructuraLike` solo para dibujar el árbol (verificar al implementar).
- No hay cambios en `RubroCreateDto`, `RubroDto`, `ProyectoUpdateDto` ni en rutas existentes: **compatibilidad hacia atrás completa**.

## Modelo de datos del borrador y migración

Tabla nueva `importaciones_estructura` (H17 obliga a clasificarla en `extract-empresas.py`: `('importaciones_estructura', 'proyecto_id', 'proyectos')`).

| Columna | Tipo | Notas |
| --- | --- | --- |
| `id` | `BIGINT GENERATED BY DEFAULT AS IDENTITY` PK | |
| `proyecto_id` | `BIGINT NOT NULL` | FK a `proyectos(id)` **ON DELETE CASCADE** (en JPA `@OnDelete(CASCADE)`) |
| `estado` | `VARCHAR(16) NOT NULL` | CHECK en `BORRADOR`, `PUBLICADO`, `DESCARTADO`, `EXPIRADO` |
| `activo_proyecto_id` | `BIGINT NULL` | = `proyecto_id` mientras `BORRADOR`, `NULL` en otro caso; **índice único**. Garantiza un borrador activo por obra y es portable (H2 no admite índices parciales) |
| `version` | `BIGINT NOT NULL` | `@Version` |
| `archivo_nombre` | `VARCHAR(255)` | Solo nombre base, saneado |
| `archivo_sha256` | `VARCHAR(64) NOT NULL` | |
| `archivo_bytes` | `BIGINT NOT NULL` | |
| `plantilla_version` | `VARCHAR(16) NOT NULL` | |
| `payload_version` | `INTEGER NOT NULL` | Versión del formato JSON |
| `payload_json` | `TEXT` (nulo tras cerrar el borrador) | Nodos, incidencias y dependencias parseados. Tope aplicado (≈ 2 MiB) |
| `total_nodos`, `total_errores`, `total_avisos` | `INTEGER NOT NULL` | |
| `creado_por_usuario_id`, `creado_en` | `BIGINT NOT NULL`, `TIMESTAMP` | |
| `expira_en` | `TIMESTAMP NOT NULL` | |
| `cerrado_en`, `cerrado_por_usuario_id` | nulos | Publicación, descarte o expiración |
| `modo_publicacion`, `agregados`, `actualizados`, `eliminados` | nulos | Auditoría de la publicación |

- Índices: único `(activo_proyecto_id)`; `(proyecto_id, estado)`.
- **Gestión del esquema**: producción/staging → migración **Flyway `V22__importaciones_estructura.sql`** aditiva y portable (sin `DROP`, sin datos); H2 local → la entidad crea la tabla por `ddl-auto: update` (no se añade a `schema.sql`, que solo contiene `ALTER`). Se debe probar que `ddl-auto: validate` acepta la entidad contra el esquema de `V22` en PostgreSQL (`TEXT` vs. tipo de `@JdbcTypeCode`; no usar `@Lob`, que en PostgreSQL mapea a `oid`). El tipo exacto de las columnas de fecha/hora debe alinearse con el que usan las migraciones existentes (p. ej. `reportes_emitidos`) al implementar.
- No se aplica ninguna migración ni se toca la base H2 persistente como parte de esta tarea; arrancar la aplicación local con la entidad nueva alteraría `data/gestiondb` por `ddl-auto: update` (requiere conciencia/aprobación al verificar).

## Seguridad y autorización

- **Autenticación**: JWT existente (`anyRequest().authenticated()`).
- **Aislamiento**: (1) ruta bajo `/api/proyectos/{id}` → `TenantAccessInterceptor.requireEmpresa`; (2) cada caso de uso repite `CurrentIdentity.requireEmpresa(empresa de la obra)`; (3) el borrador se busca por `(id, proyectoId)`: un borrador de otra obra devuelve el mismo 404 que uno inexistente; (4) el catálogo de unidades usado en la validación es el de la **empresa de la obra**; (5) el diff solo lee `rubros_obra` de esa obra.
- **Roles (recomendado, P-5)**: `@PreAuthorize("hasAnyRole('ADMINISTRADOR','JEFE_DE_OBRA')")` en todos los endpoints (también lectura del borrador), alineado con las escrituras de certificados. `COLABORADOR`, `RESPONSABLE` y `CONSULTA` quedan excluidos: el importador antiguo admite todo salvo `COLABORADOR`, lo que dejaría pasar a `CONSULTA`, cuya condición de solo lectura no es uniforme en el sistema. `SUPER_ADMIN` **no** queda habilitado por defecto (la autoridad es distinta de `ROLE_ADMINISTRADOR`); habilitarlo es una decisión del dueño.
- **Validación de archivo**: extensión `.xlsx` (sin distinguir mayúsculas); `Content-Type` declarado `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` o `application/octet-stream` (informativo, **no** autoritativo); firma `PK\x03\x04`; tamaño ≤ 2 MiB (configurable) comprobado leyendo como máximo `límite+1` bytes; nombre saneado (solo nombre base, sin caracteres de control ni separadores de ruta, ≤ 255); el archivo **nunca se escribe en disco** y no se usa su nombre para rutas. El límite global de 50 MB de multipart (H12) sigue recibiendo el cuerpo antes de que se aplique el límite de 2 MiB; se acepta como limitación.
- **ZIP/zip-bomb**: antes de POI, recorrido acotado del contenedor: ≤ 100 entradas, total descomprimido ≤ 25 MiB y ratio por entrada < 100 (medidos leyendo, no confiando en cabeceras), sin nombres con `..`, rutas absolutas o `\`; requiere `[Content_Types].xml` y `xl/workbook.xml`; entradas duplicadas ya las rechaza POI (H12). Se rechazan `xl/externalLinks/`, `vbaProject.bin`, `xl/embeddings/`, `oleObject`. No se modifican los límites globales estáticos de POI (`ZipSecureFile`), que afectarían al importador antiguo y a otras rutas.
- **XXE / entidades**: se espera que POI 5.5.1 analice XML con DOCTYPE y entidades externas deshabilitados; **no verificado**: se exige una prueba con archivos OOXML sintéticos maliciosos (DOCTYPE con entidad externa, «billion laughs»).
- **Fórmulas**: solo valor en caché, jamás evaluación (sin acceso a libros externos, sin funciones definidas por el usuario). Hipervínculos no se siguen. Texto de celdas que empiece por `= + - @` se trata como texto y se muestra escapado en la UI (React); no se exporta a CSV.
- **Límites de recursos**: ≤ 2.000 filas de datos, ≤ 30 columnas, texto ≤ 1.000 caracteres por celda, una sola hoja leída.
- **Respuestas**: errores genéricos en 5xx; nunca el mensaje interno ni contenido del archivo (a diferencia del importador antiguo, H10).
- **Auditoría**: la fila del borrador conserva quién subió (`creado_por_usuario_id`), hash/nombre/tamaño, quién y cuándo cerró y el resultado (modo, conteos). Logs estructurados (`import_estructura_creado|publicado|descartado|expirado|rechazado`) con ids, conteos y códigos de incidencia, **sin contenido de celdas ni nombres de nodos**. No existe una tabla de auditoría genérica en el código.
- **Interruptor**: `app.features.importacion-estructura-enabled` (patrón de `app.features.gastos-fijos-enabled`, H18); si está apagado, 503 `FEATURE_TEMPORARILY_UNAVAILABLE`. Permite desplegar sin exponer (P-17).
- **Datos de producción**: no se usan datos reales en pruebas; solo libros sintéticos en memoria.

## UX frontend

Ruta nueva `app/proyectos/[id]/actividades/importar/page.tsx`, con punto de entrada «Importar desde Excel» en Estructura de actividades (visible solo con la capacidad `importStructure`; el backend es la autoridad) y, si hay borrador activo, un aviso «Tenés un borrador de importación (vence el …)» con «Continuar» / «Descartar». Reutiliza componentes `ui/` (Card, Button, Wizard/Stepper, Toast, LoadFailure, EmptyState, Spinner), `lib/confirm` (SweetAlert) y `apiErrorMessage`. Sin librerías nuevas ni gestor de estado nuevo.

1. **Subir**: «Descargar plantilla» (llama a `GET …/plantilla`), zona de arrastre/selector `.xlsx` (validación previa de extensión y tamaño solo como ayuda; el servidor decide), «Subir y validar». Estados: cargando, error (mensaje y `code`), éxito. Si hay `BORRADOR_ACTIVO`: ofrecer «Continuar con el borrador» o «Descartarlo y subir este» (`reemplazarBorrador=true`, con confirmación).
2. **Revisar** (pestañas):
   - **Árbol**: rubro → subrubro → nivel con código, nombre, cantidad/unidad, HH recalculadas, personas, fechas y dependencias; plegable y operable con teclado (patrón de `disclosure-row`); insignias de errores/avisos por nodo.
   - **Incidencias**: tabla filtrable por severidad, con «fila N · celda E7»; al elegir una se resalta el nodo. Banner fijo: «Las dependencias se validan pero no se guardan en la plataforma».
   - **Cambios (diff)**: conteos (agregados/modificados/eliminados), clasificación y lista agrupada con antes/después.
3. **Publicar**: botón deshabilitado si `publicable=false` (se explica por qué). Según clasificación:
   - `SOLO_AGREGA`/`SIN_ESTRUCTURA_PREVIA`: «Se agregarán N nodos» + confirmar.
   - `CAMBIOS_MENORES`: elegir «Solo agregar lo nuevo» o «Reemplazar» (si elimina nodos: «Se eliminarán N nodos: …»).
   - `MUY_DIFERENTE`: diálogo explícito «La estructura del archivo es muy diferente (X % de la estructura actual cambiaría). ¿Querés reemplazarla?» que exige **escribir REEMPLAZAR**; opción «Cancelar» destacada. Nunca hay borrado por defecto.
   - Un clic = una petición (deshabilitar durante el envío). 409 `DIFF_DESACTUALIZADO`: recargar diff y pedir confirmar de nuevo. Éxito: resumen (agregados/actualizados/eliminados) y recarga fresca de la obra (`proyectosApi.getByIdFresh`).
4. **Descartar** disponible en todo momento (con confirmación); el borrador vive en el servidor, no hay estado local sin guardar.

Accesibilidad: foco gestionado en diálogos, texto de estado anunciado, no depender solo del color para severidad. El FE nunca convierte un fallo en un resultado de apariencia válida (AGENTS FE). No se registra en consola contenido del archivo.

Cambios FE adicionales: `capabilities().importStructure` y la ruta en `restrictedTeamRoute` (`lib/permissions.ts`); la invalidación de caché ya cubre `POST/DELETE /api/proyectos/{id}/…` (alcance «obra») sin cambios en `classifyWrite`. Antes de implementar, leer la guía vigente de Next.js en `node_modules/next/dist/docs/` (aviso del AGENTS FE; el proyecto usa Next 16.4.3 y React 19.3.1).

## Plantilla descargable

Generada por el backend con POI al pedirla (no se versiona un binario; así no se desincroniza del parser). Hoja `Estructura`: filas 1–3 (título, instrucciones breves, «Versión de plantilla: 1»), encabezados en la fila 4 con los textos exactos de la tabla, columna Código con formato de texto, fechas `dd/mm/yyyy`, números con formato numérico; filas de ejemplo ficticias solo en la hoja `Ejemplo` (no en `Estructura`, para no importarlas por accidente) y hoja `Instrucciones` (jerarquía, reglas de dependencias, «los totales se recalculan»). Sin fórmulas en agrupadores. Opcional: validación de datos de «Unidad» con el catálogo de la empresa (no requerido para aprobar).

## Compatibilidad con el importador existente

| Opción | Evaluación |
| --- | --- |
| Reutilizar `ExcelImportService` | **No**: índices de columna fijos, otro modelo (`Entrega`/`Tarea`/`Rendimiento`), crea proyectos sin empresa, devuelve `getMessage()` al cliente y no tiene borrador ni diff |
| Reemplazarlo | **No** en esta spec: se desconoce si `/import` sigue en uso y su comportamiento de sobrescritura no está verificado (FE 005); romperlo no está autorizado |
| **Coexistir (recomendada)** | Código nuevo en un paquete propio (`infrastructure/excel/estructura/`); `ImportController`, `ExcelImportService`, `/api/import/excel`, `/import`, sus tests (`ExcelParserSecurityTest`, `context-queries.test.cjs`) **no se modifican** |
| Deprecar después | Spec aparte tras P-11: etiquetar `/import` como heredada, corregir el alta de proyecto sin empresa (H10) o retirarla |

Compartido sin cambios: dependencia POI 5.5.1 (sin dependencias nuevas). No se comparte código del servicio antiguo.

## Estrategia de pruebas y verificación (no ejecutada)

**Backend** (`gestion_be`; comandos de `gestion_be/AGENTS.md`; ninguno se ejecuta sin aprobación de alcance):

- Sin contexto (JUnit + Mockito, libros `.xlsx` sintéticos en memoria): parser de celdas (números es-AR, ambiguos, fechas, texto/numérico de códigos, fórmulas con/sin valor en caché, celdas de error), normalización de encabezados, localización de cabecera, reglas de la tabla de incidencias, gramática y grafo de dependencias (ciclos, auto-referencia, agrupadores, «Inicio de obra»), inferencia de variante, mapeo a `RubroCreateDto`, diff (A/S/M/R, umbral en bordes: 29,9 %/30 %), huella estable, bloqueos, validador de archivo (extensión, firma, ratio, entradas, vínculos externos, macros, XXE/billion-laughs sintéticos), generador de plantilla (ida y vuelta: la plantilla generada es leída por el parser).
  `mvn -Dtest=ImportacionEstructuraParserTest,ImportacionEstructuraDiffTest,DependenciasEstructuraTest,ArchivoXlsxValidatorTest test`
- HTTP/integración (inicia Spring + H2 aislado; requiere aprobación de ejecución): autorización por rol × endpoint, aislamiento entre empresas y entre obras (403 vs 404 según checklist), borrador no modifica `rubros_obra`, publicar AGREGAR/REEMPLAZAR, atomicidad (fallo a mitad ⇒ nada cambia y el borrador sigue `BORRADOR`), bloqueos por tareas/restricciones/certificados/horas, un borrador activo, expiración, descarte idempotente, huella obsoleta, `@Version`, eliminar obra ⇒ borradores eliminados.
  `mvn -Dtest=ImportacionEstructuraHttpTest test` (siguiendo `docs/testing-checklist.md`: H2 en memoria generado, seeders suprimidos, fixtures confirmados antes de las peticiones).
- PostgreSQL (opcional, por patrón `Quality14PostgresTest`: opt-in por propiedad y PostgreSQL desechable): Flyway `V22` + `ddl-auto: validate`, unicidad concurrente del borrador activo, publicaciones concurrentes y publicación vs. `PUT /api/proyectos/{id}` (H2 no demuestra el bloqueo en otro motor). Se añade la aserción de `V22` a `PostgresMigrationSourceTest`.
- Regresión: `RubroEstructuraUseCasesTest`, `RubroEstructuraPersistenciaTest`, `EstructuraRubrosTest`, `RubroPlanningTest` y `ExcelParserSecurityTest` deben seguir pasando; `python3 -B -m unittest discover -s scripts/tests -p test_extract_empresas.py -v` (clasificación de la tabla nueva).
- Verificación final: una sola `mvn verify` (incluye HTTP). No se afirma éxito sin haberla ejecutado.

**Frontend** (`gestion_fe`; AGENTS FE): `./node_modules/.bin/tsc --noEmit --incremental false` y `npm run build`. No hay script npm de pruebas unitarias; el repositorio ejecuta `tests/*.test.cjs` con `node --test` (CI-AUTOMATION-13), con el cargador `tests/support/load-ts.cjs`: se propone `tests/importacion-estructura.test.cjs` (helpers puros: árbol del borrador, conteos, reglas de habilitación del botón, textos de confirmación, mapeo de `code` de error) y la ampliación de `tests/team-permissions.test.cjs` (ruta y capacidad). Comando: `node --test tests/importacion-estructura.test.cjs`. Se propone un recorrido E2E Playwright (subir → revisar → publicar) solo si el dueño lo aprueba; no se asume que exista infraestructura E2E lista para este flujo.

**Entre repositorios**: contrato verificado campo a campo contra los ejemplos JSON de este documento (pruebas de contrato en BE con los mismos ejemplos; tipos TS revisados contra los records). Resultados por repositorio por separado.

## Criterios de aceptación (verificables)

Archivo y parseo

1. Un `.xlsx` de la plantilla v1 válido genera un borrador con 201; la estructura real de la obra y todas las tablas de negocio quedan **idénticas** (recuentos y valores) antes y después de subir, revisar y descartar.
2. Se rechazan sin crear borrador: archivo vacío, extensión distinta de `.xlsx`, firma no ZIP, > 2 MiB, ZIP con ratio ≥ 100 o > 100 entradas o ruta con `..`, con `xl/externalLinks/`, con `vbaProject.bin`, con DOCTYPE/entidades externas, sin cabecera, sin una columna requerida, con cabecera duplicada, > 2.000 filas.
3. Las columnas se leen por encabezado normalizado: reordenar columnas o mover la cabecera a otra fila dentro de las 10 primeras produce el mismo resultado; las tildes/mayúsculas/espacios en encabezados no importan.
4. Cada incidencia de la tabla de reglas se produce con su `codigo`, severidad, fila y celda correctas (una prueba por código), incluidos: duplicados (cada aparición), padre inexistente, unidad requerida, negativos, personas inválidas, fechas inexistentes (31/02), fin < inicio, código numérico no entero, número ambiguo `1.234`, fórmula sin valor, celda de error.
5. Números `12,5`, `1.234,56` y numéricos nativos se leen igual; `dd/MM/yyyy` y fechas nativas de Excel se leen igual; ningún otro formato de fecha en texto se acepta.
6. Dependencias: `3.1.2, 5.1, 5.2`, `3.2.1 (+10 d)` y `Inicio de obra` se resuelven; una referencia inexistente, una sintaxis inválida, la auto-dependencia y un ciclo A→B→C→A producen ERROR con el camino del ciclo; «Inicio de obra» no participa de ciclos.
7. Totales: ningún valor de agrupador ni «HH totales» del archivo se persiste; las HH publicadas de una hoja = APU × cantidad (misma regla que `RubroPlanning`); discrepancias generan AVISO; con APU ausente y HH + cantidad presentes se derivan los rendimientos.
8. La variante se infiere por rubro (3 capas si hay hojas de profundidad 3); el resultado pasa por `EstructuraRubros.resolver`; un archivo equivalente cargado por el flujo manual produce las mismas filas `RubroObra` (comparación campo a campo).

Diff y publicación

9. Para estructura vacía, solo-agregados, cambios menores y muy diferente (casos con 29,9 % y 30 %), el `clasificacion`, los `modosPermitidos` y las confirmaciones requeridas coinciden con la tabla.
10. `AGREGAR` inserta solo los nodos nuevos y no modifica ni elimina ninguna fila existente (comparación antes/después).
11. `REEMPLAZAR` conserva los `id` de los nodos con el mismo código; las tareas, restricciones, certificados y registros de horas vinculados siguen enlazados; los campos fuera de plantilla (contratista, empresa, contrato, frentes) de los nodos existentes no cambian; una celda opcional vacía del archivo (APU, personas, fechas) conserva el valor existente.
12. **No se elimina ningún nodo** sin `modo=REEMPLAZAR`, `confirmaciones.eliminaciones` igual al R que recalcula el servidor y, en `MUY_DIFERENTE`, `textoConfirmacion="REEMPLAZAR"`; en cualquier otro caso 400 `CONFIRMACION_REQUERIDA` y ninguna fila cambia.
13. Un nodo eliminado/alterado con tareas, restricciones, avance en certificado o registros de horas ⇒ 409 `BLOQUEOS_DE_PUBLICACION` y nada se modifica.
14. Atomicidad: si cualquier paso de la publicación falla (violación inyectada a mitad), `rubros_obra`, `proyectos.horas_previstas` y el estado del borrador quedan como antes; no hay publicación parcial.
15. Una edición manual de la estructura posterior a la revisión hace que publicar devuelva 409 `DIFF_DESACTUALIZADO` sin cambios; dos publicaciones simultáneas del mismo borrador: exactamente una tiene éxito y la otra recibe 409.
16. Tras publicar, `proyecto.horasPrevistas` = suma de HH de las hojas (misma regla que la actualización manual).

Ciclo de vida y concurrencia

17. Un segundo borrador en la misma obra devuelve 409 `BORRADOR_ACTIVO` (también con subidas simultáneas, garantizado por el índice único); con `reemplazarBorrador=true` el anterior pasa a `DESCARTADO`.
18. Un borrador con `ahora > expiraEn` pasa a `EXPIRADO` al acceder, no es publicable y su payload se anula; descartar es idempotente; publicar un borrador `PUBLICADO` o `DESCARTADO` devuelve 409.
19. Eliminar la obra elimina sus borradores (H2 de prueba y PostgreSQL).

Seguridad

20. Un usuario de otra empresa recibe 403 en todos los endpoints; el mismo usuario con `draftId` de otra obra de su empresa recibe 404 idéntico al de inexistente; `COLABORADOR`, `RESPONSABLE` y `CONSULTA` reciben 403; ningún endpoint filtra datos de otras empresas.
21. Ningún error 4xx/5xx incluye contenido de celdas, rutas, trazas ni nombres internos; los logs de la funcionalidad no contienen valores de celdas.

Cronograma, API y frontend

22. Con DC-02 A: tras publicar, la hoja con inicio y fin tiene esas fechas y `cronogramaManual=true`; sin fin, `cronogramaManual=false`; los agrupadores conservan sus fechas previas; **verificación manual aprobada** de que Cronograma/Gantt muestran el rango importado.
23. Los DTO reales coinciden con los ejemplos JSON de este documento y con los tipos TS; `tsc --noEmit` y `npm run build` pasan; helpers cubiertos por `node --test`.
24. La pantalla cubre y se prueba: carga, vacío, error, éxito, subida con errores (publicar deshabilitado y razón visible), diff por clasificación, diálogo `MUY_DIFERENTE` que exige escribir `REEMPLAZAR`, `DIFF_DESACTUALIZADO`, `BORRADOR_ACTIVO`, descarte.
25. `POST /api/import/excel`, `/import` y sus pruebas existentes mantienen su comportamiento (sin modificaciones en esos archivos).
26. `V22` aplica sobre PostgreSQL con `ddl-auto: validate` sin error y `test_extract_empresas.py` sigue verde con la tabla clasificada.

## Riesgos

| Riesgo | Mitigación |
| --- | --- |
| Código numérico `3.10` leído como 3,1 | Columna formateada como texto en la plantilla + `CODIGO_NUMERICO_AMBIGUO` |
| Refactor de `UpdateProyectoUseCase` para compartir la sincronización (plan DT-7) puede alterar el flujo manual | Extracción mecánica con pruebas de regresión existentes; alternativa documentada |
| Interacción `cronogramaManual` ↔ UI de Cronograma (H8) | Decisión explícita DC-02 y criterio 22 con verificación manual |
| Se pierden las dependencias al publicar y el usuario cree lo contrario | Aviso permanente en revisión y publicación; DC-03 |
| Un borrador es persistente y contiene estructura de la obra | Un solo borrador, expiración, payload anulado al cerrar, sin datos personales en el contenido; cascada al borrar obra |
| `TEXT`/JSON difiere entre H2 y PostgreSQL | Prueba de `validate` contra V22; sin `@Lob` |
| H2 no demuestra el bloqueo ni el índice único concurrente | Prueba PostgreSQL opt-in |
| Arrancar el perfil local altera el H2 persistente (`ddl-auto: update`) | Declararlo; verificar solo en bases efímeras |
| Fuentes «decompiled» (H10) y estilo heterogéneo | Código nuevo en paquetes propios; no editar esos archivos salvo un handler en `GlobalExceptionHandler` (ver plan) |
| `/api/import/excel` crea proyectos sin empresa (H10) | Fuera de alcance; se informa en P-11 |
| 2 MiB / 2.000 filas insuficientes para obras reales | Parametrizable; P-14 |
| POI DOM carga el libro en memoria | Tope de 2 MiB y validación previa del contenedor; evaluar SAX si crece el límite |
| `extract-empresas.py` aborta ante la tabla nueva (H17) | Clasificarla en la misma entrega |

## Preguntas abiertas y aprobaciones requeridas

| # | Pregunta | Recomendación |
| --- | --- | --- |
| P-1 | En hojas de 3 capas (`RUBRO_SUBRUBRO_NIVEL`) el servidor usa el **nombre** de la hoja como nivel. ¿«Rubro / Subrubro» de una hoja repite el subrubro, describe la actividad o es el nombre del nivel? | `nombre` = columna «Nivel» si existe, si no «Rubro / Subrubro»; aviso `NOMBRE_HOJA_DISTINTO` si ambas difieren |
| P-2 | ¿Se necesita `RUBRO_NIVEL_SUBRUBRO` en la plantilla (columna «Variante» opcional)? | No en v1 |
| P-3 | «Tipo de APU»: ¿qué valores admite y mapea a `tipoContratista` (`PROPIA`/`SUBCONTRATO`)? | Informativo en v1 |
| P-4 | ¿30 % de cambio estructural es el umbral correcto para «muy diferente»? | Sí, configurable |
| P-5 | Roles autorizados y papel de SUPER_ADMIN | ADMINISTRADOR + JEFE_DE_OBRA; sin SUPER_ADMIN |
| P-6 | Aprobar DC-02 (opción A) tras revisar cómo Cronograma muestra filas manuales (H8) | A |
| P-7 | Dependencias: ¿persistir? ¿semántica fin-a-inicio y días corridos para «(+10 d)»? ¿ERROR para sin resolver/ciclos? | No persistir; FS y días corridos; ERROR |
| P-8 | Confirmar DC-01 (recalcular) y la tolerancia de discrepancia | Recalcular; max(0,01; 0,5 %) |
| P-9 | TTL del borrador (7 días) y retención de metadatos de publicación | 7 días; metadatos permanentes, payload anulado al cerrar |
| P-10 | Unidad fuera del catálogo: ¿AVISO o ERROR? ¿Crear la unidad automáticamente? | AVISO; no crear |
| P-11 | ¿Sigue vigente `/import` / `/api/import/excel`? ¿Se deprecará? ¿Se corrige el alta sin empresa? | Spec aparte |
| P-12 | ¿Puede publicar/descartar cualquiera con rol autorizado, o solo quien subió? | Cualquiera autorizado |
| P-13 | Aprobar el refactor acotado de `UpdateProyectoUseCase` (plan DT-7) | Sí, o la alternativa B documentada |
| P-14 | Tamaño de las obras reales (nodos, MB) para validar 2.000 filas / 2 MiB | Medir con una muestra |
| P-15 | Confirmar que contrato/contratista/empresa por actividad quedan fuera de la plantilla v1 | Fuera de alcance |
| P-16 | Proveer un Excel real de ejemplo (sintético) para validar el formato; no se abrieron `PREV VS REAL.xlsx` ni el PDF de la raíz | Necesario antes de cerrar la spec |
| P-17 | Estado del interruptor `app.features.importacion-estructura-enabled` en producción | Apagado hasta cierre y revisión |

**Aprobaciones requeridas antes de implementar:** esta especificación (DC-01…DC-04, P-1…P-17 relevantes), el plan, el alcance de ejecución de pruebas (`mvn` focalizado, HTTP aislado, PostgreSQL desechable opcional, `node --test`, `tsc`, `next build`) y, por separado, cualquier migración sobre una base persistente. Ninguna aprobación se infiere del silencio.
