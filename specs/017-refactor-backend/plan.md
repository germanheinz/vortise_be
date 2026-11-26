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
