# Plan de refactor del backend

## Piloto de Previsto vs. Real

1. Confirmar el diff local de `GetPrevistoRealUseCase` como línea base. Antes del refactor, revisar la cobertura y añadir pruebas de caracterización donde falte evidencia. Priorizar precedencia de días, nulos frente a cero, valores no finitos, dotación heredada y orden de rubros y cargas.
2. Hacer que `execute` muestre la secuencia de lectura del proyecto, resolución de jornada y costos, agrupación por rubro y construcción del resumen. Usar imports explícitos y nombres como `costoHoraOficial` y `tareasPorRubro`. Extraer métodos privados para las etapas que hoy están mezcladas dentro del stream.
3. Extraer cálculo y agregación del anexo a una clase de paquete sin dependencias de Spring ni repositorios. Mantener los records públicos, constructores y la firma de `execute`. Reutilizar `PrevistoRealCalculator` y `EstructuraRubros`. Conservar la posición y frecuencia de lectura de la fecha, la suma en el mismo orden y el comportamiento del agrupamiento con claves nulas. Mantener el acceso a datos dentro de la transacción existente.
4. Probar primero el caso de uso y sus consumidores directos. Revisar el diff comparando las ramas y operaciones con la línea base. Verificar vía HTTP la respuesta y los accesos y ejecutar la verificación completa según la aprobación de ejecución. Cerrar con revisión independiente.

## Candidatos posteriores

| Orden propuesto | Evidencia | Siguiente corte |
| --- | --- | --- |
| 2 | `CreateObraTareaUseCase` repite cálculo de métricas en alta y edición. | Caracterizar ambas rutas y extraer el cálculo compartido conservando validaciones, bloqueos y redondeo. |
| 3 | `UpdateProyectoUseCase` reúne actualización, cambios de empresa, estructura y contratistas. | Revisar el flujo transaccional y extraer una responsabilidad por cambio, empezando por el mapeo de rubros y sus validaciones. |
| 4 | `CertificadoObraUseCase` reúne consultas, armado de detalle, edición de corte y cierre. | Separar primero la composición de vistas después de caracterizar resultados por empresa, bloqueos y estados. |
| 5 | `VincularTelegramUseCase` importa directamente `JpaVinculoTelegramRepository`. | Revisar un puerto específico con el patrón existente y preservar consultas, bloqueos y semántica transaccional. Aprobación independiente para ese cambio. |

El dominio contiene modelos anotados con JPA y el proyecto ya tiene puertos. La extracción de puertos se propone donde existe una dependencia concreta hacia infraestructura. Separar masivamente entidades de dominio y persistencia queda fuera de este plan por su impacto en mappings, carga de relaciones y persistencia.

## Segundo corte propuesto

Extraer un método privado para aplicar los datos compartidos después de validar la actividad y otro para derivar las métricas de avance. Mantener explícitas en las entradas las validaciones específicas y la asignación de metadatos del alta. Conservar la secuencia de consultas y mutaciones del bloque común en este corte. Limitar los cambios a `CreateObraTareaUseCase.java` y las pruebas de caracterización que falten.

## Consolidación de datos de avance

1. Caracterizar las respuestas actuales de alta, edición, listado y resumen de empresa. Cubrir cargas vinculadas, cargas históricas sin rubro, subcontratos y cambios del presupuesto. Revisar por separado las fórmulas de Previsto vs. Real y de la respuesta de tareas.
2. Dar una ubicación explícita a la proyección de métricas que ya usa la respuesta de tareas y el resumen de empresa. Conservar su fórmula y el respaldo histórico. Revisar importaciones, seeders y respaldos antes de decidir cuándo dejar de materializar los derivados en cargas nuevas.
3. Evaluar el retiro del almacenamiento duplicado en una fase propia. Mantener las columnas durante la transición y definir la recuperación de datos históricos. Evaluar aparte la consolidación del total de horas de Proyecto, que actualmente admite entrada directa cuando no hay registros.

### Corte propuesto para dejar de materializar derivados

1. Caracterizar la respuesta y la entidad guardada en alta, edición estándar y edición de una carga antigua. Cubrir rechazo de cantidad prevista inválida, respaldo sin rubro y cambio de presupuesto. Conservar la fórmula y el orden de validación de la proyección y de las protecciones de escritura.
2. En `CreateObraTareaUseCase`, quitar las asignaciones duplicadas y el cálculo que solo alimenta las columnas derivadas. Conservar las etiquetas del rubro y los valores de respaldo que ya tengan las filas existentes. La API sigue resolviendo el rubro y usando `ObraTareaMapper`.
3. En `DemoOperationalSeed`, calcular las cantidades para el resumen al generar las cargas y pasarlas en memoria, con la fórmula, elegibilidad y orden de suma actuales. Conservar la detección de marcadores antiguos. Cuando uno de esos marcadores se reutiliza como carga vinculada, limpiar su porcentaje de marcador. Las cargas demo ya existentes se preservan.
4. Comparar antes y después los resultados de las pruebas unitarias de tareas, resumen, cálculo y datos demo. Revisar la compatibilidad de lectura con filas históricas y ejecutar verificación HTTP solo con aprobación de ejecución. Postergar cualquier retirada de columnas hasta que exista una estrategia para respaldos con la huella anterior.

## Inventario de duplicaciones del backend

El barrido estático del árbol Java principal cubrió 356 archivos y 19.001 líneas. Se buscaron bloques repetidos y se contrastaron candidatos con sus consumidores, pruebas y mappings. Las coincidencias sintácticas se revisaron por significado antes de recomendar una extracción.

| Prioridad | Evidencia | Simplificación y compatibilidad |
| --- | --- | --- |
| 1 | `CreateProyectoUseCase` y `UpdateProyectoUseCase` repiten validaciones de fechas y superficies, y el mapeo de plantas. | Extraer las reglas y el mapeo comunes. Conservar orden, tipo y texto de errores, listas mutables y el comportamiento de plantas omitidas frente a lista vacía. Mantener en los casos de uso permisos y mutación de colecciones JPA. |
| 2 | `ProyectoMapper` repite la construcción del mismo DTO en resumen y detalle. | Compartir un constructor privado del DTO con los datos comunes. Conservar los campos nulos del resumen, las listas del detalle y el orden natural de rubros. El resumen debe seguir sin recorrer colecciones del detalle. |
| 3 | Alta y edición de proyecto repiten la resolución y validación de subcontratistas por empresa, tipo y selección. | Extraer la resolución compartida y dejar en cada flujo el origen de los IDs y las protecciones específicas. Caracterizar duplicados, IDs inexistentes, selección implícita y contratistas todavía asignados. |
| 4 | `UpdateProyectoUseCase` consulta dos veces los avances en una rama y repite búsquedas de rubros enviados. | Evaluar una sola lectura de avances y un índice de rubros conservando la primera coincidencia. Revisar antes el autoflush de JPA, la precedencia de errores y el comportamiento con IDs repetidos. |
| 5 | `RegistroHorasService` y `UpdateProyectoUseCase` repiten la suma y normalización del total. | Compartir el cálculo usando la consulta existente. Conservar la entrada manual en edición cuando no hay registros y el cero al borrar el último registro. Mantener bloqueos y sincronización en sus puntos actuales. |
| 6 | Dashboard y consulta de restricciones repiten los estados de cierre y su normalización. | Compartir solo la clasificación textual. La consulta usa fecha de entrega y corte, y la actualización tiene una regla propia para `RESUELTO`. Conservar las tres decisiones. |
| 7 | `ObraTareaMapper`, `PrevistoRealCalculator` y el cálculo del anexo comparten insumos de productividad. | Comparar con una matriz de casos antes de extraer. El indicador de productividad usa previsto dividido por real, y el rendimiento de la tarea usa real dividido por previsto. También difieren los redondeos y las reglas con horas faltantes. |
| 8 | `Ingreso` y `CostoIndirecto` tienen forma JPA y código auxiliar muy parecidos. Los DTO de alta y edición también comparten campos. | Mantener significados y tablas. Evaluar limpieza de código repetitivo solo donde reduzca mantenimiento, sin herencia ni cambios de contrato motivados por el parecido. |

El primer corte propuesto abarca validación de fechas y superficies y mapeo de plantas de proyecto. Verificar alta con plantas omitidas, edición con plantas omitidas y vacías, y precedencia de validaciones. Mantener DTO, permisos, transacciones y persistencia. Las coincidencias de redondeo trivial, constructores de inyección y métodos de repositorio se dejan fuera del primer corte.

### Consolidación de ProyectoMapper

Usar una construcción común de `ProyectoDto` con las dos entradas actuales y resolver las colecciones únicamente al solicitar el detalle. Verificar el orden de rubros y el contrato de las colecciones en las dos entradas antes y después.

### Consolidación de subcontratistas de proyecto

Extraer las dos búsquedas comunes de catálogo a una clase de paquete que recibe el repositorio existente. Conservar el origen de IDs y las protecciones específicas dentro de cada caso de uso. Mantener las consultas, transacciones, autorizaciones y constructores existentes.

### Cálculo común de horas reales

Compartir la consulta de suma, la normalización del agregado nulo y el redondeo en `RegistroHorasRules`. Mantener en cada llamador la decisión de calcular, el bloqueo, la sincronización de JPA y el guardado. Conservar el esquema y el contrato HTTP.

## Migración por módulos a Controller → Service → Repository

1. En Registro de horas, conectar los tres endpoints de escritura de `RegistroHorasController` con `RegistroHorasService`. Retirar las tres clases que solo delegan, comprobando sus referencias en todo el backend. Mantener las dos consultas en sus entradas existentes durante este corte.
2. Evaluar consolidar las consultas de Registro de horas y después los CRUD de tareas, restricciones y gastos fijos. Para cada módulo, revisar qué lectores y escritores comparten permisos, transacciones y respuestas. Mover mappers y reglas al paquete del módulo cuando ayude a seguir el flujo y no rompa consumidores.
3. Revisar Proyectos y Empresa por separado. Mantener servicios con responsabilidades propias, como certificados, reportes y Previsto vs. Real. Conservar interfaces de repositorio, adaptadores y modelo de datos.
4. Verificar cada corte con pruebas focalizadas y revisar HTTP al aprobarse el arranque de Spring. La revisión independiente y el cierre documentan resultados y limitaciones por módulo.

### Servicios consolidados

- `RegistroHorasService` reúne consultas y mutaciones, y conserva la transacción en los tres métodos de escritura.
- `ObraTareaService` reúne el CRUD. Previsto vs. Real conserva su servicio de indicadores. El listado permanece en una transacción de solo lectura, el alta y la edición mantienen sus transacciones y la baja sigue usando la del repositorio.
- `RestriccionService` reúne las operaciones sobre restricciones. `ComentarioRestriccionService` concentra las de comentarios. Las operaciones de chat conservan sus reglas y llaman al servicio de restricciones.
- `GastoFijoService` comparte el mapeo entre listado y guardado. El controller verifica la disponibilidad de la funcionalidad antes de delegar.
- `ProyectoConsultaService` agrupa cuatro consultas y lo comparten los controllers de proyecto y empresa. `EmpresaService` agrupa lectura y guardado de empresa. Los flujos de creación, edición y borrado de proyecto y la baja de empresa conservan sus servicios especializados por sus reglas y dependencias.

### Paquetes de aplicación

Mover `EmpresaMapper` a `application.mapper` y `EmpresaService` a `application.service`. El mapper expone su método estático para permitir el acceso desde el servicio y mantiene privado su constructor. El servicio conserva sus consumidores y el wiring de Spring con los paquetes nuevos.

Aplicar la organización a todo el antiguo paquete `application.usecase`. Mantener nombres de clase y agrupar las pruebas por el tipo principal que prueban. Cambiar visibilidad solo en las entradas necesarias entre paquetes. Comparar los cuerpos con la revisión previa y compilar desde cero para comprobar las referencias.

Normalizar el sufijo de los servicios y usar nombres específicos para altas, ediciones, bajas y consultas que tengan responsabilidades distintas. Mover `EstructuraRubros` y su prueba a `domain.rule`. Retirar `CostoCalculadorService` tras comprobar referencias, configuración y consumidores. Actualizar imports y ejecutar compilación limpia y pruebas sin contexto.

### Retiro de seeders de negocio demo

1. Retirar `DataSeeder`, `DataSeederRunner`, `DemoOperationalSeed`, `UsuarioDemoSeeder`, `SeedProfiles` y la entrada de reset demo. Eliminar pruebas exclusivas de esas funciones. En las pruebas de producción, retirar sus mocks y conservar la creación local de fixtures y las protecciones H2. Mantener `SeedRunPolicy` y sus implementaciones mientras los seeders operativos las consuman.
2. Revisar `E2eTeamFixtures` junto con el lanzador E2E del frontend. Trasladar el aprovisionamiento a código de pruebas compatible con el lanzador antes de quitar su componente del backend. La configuración de aislamiento E2E se conserva durante esa transición.
3. Retirar la API de gastos fijos globales y su pantalla y cliente del frontend en un corte coordinado. Evaluar entidad, adaptador y respaldo H2 por separado. El primer cambio de código conserva la tabla y la lectura de respaldos históricos. Mantener el resto de modelos y flujos de Excel.
4. Comparar endpoints de producción y ejecutar por separado las comprobaciones de backend y frontend. La autorización de ejecución debe cubrir los contextos reales seleccionados. El alcance no incluye limpiar bases persistentes, ejecutar resets, migraciones destructivas ni publicar cambios.

### Retiro de Telegram y del alta por chat

- Backend: retirar el controller, comando, conversación y servicio de vinculación de Telegram y su excepción de autorización HTTP. Retirar el POST de restricciones por chat, el servicio idempotente y repositorios exclusivos. Conservar el GET y el servicio de consulta usados por el asistente web. Retirar el código de pruebas exclusivo y conservar las pruebas de restricciones normales.
- Frontend: quitar el modo de alta del asistente, el POST y la invalidación de caché asociada. Conservar las acciones de lectura y las pantallas de restricciones normales. Retirar el workflow de Telegram versionado y sus pruebas exclusivas. Revisar los cambios de autenticación locales antes de editar archivos.
- Mantener las migraciones aplicadas y los mappings históricos que necesita la huella de respaldos. No modificar filas existentes. No operar sobre workflows remotos de n8n ni Telegram; cualquier retiro remoto se coordina por separado.
- Verificar contratos de lectura y alta regular, referencias obsoletas y resultados por repositorio. La ejecución de contextos Spring requiere autorización según las reglas del backend.
