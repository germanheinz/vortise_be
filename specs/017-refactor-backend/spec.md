# Refactor de legibilidad y arquitectura del backend

El objetivo es que cada caso de uso exprese su flujo de negocio y que los cálculos y adaptadores tengan responsabilidades reconocibles. El piloto será Previsto vs. Real. Los demás módulos son candidatos a revisiones posteriores con criterios propios.

## Criterios de aceptación del piloto

- El endpoint de indicadores y el dashboard conservan sus resultados, campos, orden, valores nulos, errores y límites de acceso por empresa y obra. Se conserva la consulta transaccional de solo lectura y el contrato Java que usan sus consumidores.
- Se conservan la inclusión de hojas y rubros con cargas, el conteo de tareas sin vincular y el indicador de desencofrado. La dotación se resuelve con la precedencia del rubro, sus niveles intermedios y la raíz.
- Se conservan las fórmulas, el orden de las operaciones y el tratamiento de cero, nulos, valores negativos y valores no finitos. Se cubre la diferencia entre agregar cantidades y agregar horas. Los costos siguen requiriendo una única categoría habilitada con el nombre correspondiente.
- La línea base propuesta es el árbol de trabajo actual. En ese código, las horas previstas a fecha intentan usar días transcurridos desde el inicio planificado, luego días de la tarea y finalmente horas previstas del rubro. El campo de días sigue la asignación de cada rama, incluso si un cálculo devuelve un valor no finito. La fecha sigue dependiendo de la zona horaria del proceso. La línea base queda pendiente de confirmación con la aprobación del plan.
- La implementación debe permitir leer el recorrido principal sin recorrer las fórmulas. Los cálculos extraídos se pueden verificar con JUnit sin Spring ni base de datos. El alcance excluye cambios de esquema, dependencias, despliegues y frontend.

La especificación funcional existente sigue en [Previsto vs. Real](../002-previsto-real/spec.md).

## Alta y edición de tareas

- Alta y edición comparten la asignación de campos y el cálculo de cantidad equivalente y porcentaje. Conservan los resultados, nulos, redondeo, errores, mensajes y orden de validación.
- En edición se mantiene la comprobación de pertenencia a la obra, la autoría para Jefatura y el bloqueo del avance anterior antes de validar el nuevo. Alta conserva fecha y usuario de registro. Edición conserva esos metadatos y los días existentes cuando el DTO los omite.
- Se preservan los datos derivados del rubro, los subcontratos sin horas, el desencofrado sin cantidad ejecutada y una sola llamada a guardar después de las validaciones.

## Consolidación de datos de avance

- Conservar petición y respuesta de alta, edición y listado de tareas, incluidos los campos que el asistente de carga envía como vista previa. Conservar errores de validación, redondeo, nulos y resultados después de editar el presupuesto de un rubro.
- Las tareas vinculadas presentan las métricas derivadas del rubro y las horas de la carga. Las tareas antiguas sin rubro resoluble conservan el respaldo almacenado.
- Conservar etiquetas registradas e historial de reportes. Mantener separados los totales de registro horario y de avance hasta confirmar su relación operativa.

### Retiro gradual de escrituras duplicadas

- Las nuevas tareas vinculadas creadas por API conservan la respuesta actual y se guardan sin cantidad prevista ni porcentaje de cumplimiento materializados. Se mantienen los campos de petición y las validaciones actuales.
- En edición se preserva el respaldo persistido de filas históricas. Cuando una tarea sin rubro se vincula por primera vez, la respuesta pasa a usar el mismo cálculo de lectura que usa hoy una tarea vinculada.
- El generador demo conserva el reconocimiento de cargas antiguas, su idempotencia y los totales de los registros horarios de resumen. Los valores necesarios para calcular esos resúmenes pueden mantenerse en memoria durante la generación.
- Se conservan las columnas para leer datos históricos y mantener la compatibilidad de formato de los respaldos. La verificación compara las respuestas y los resúmenes antes y después del cambio.

## Próximo corte de simplificación de proyectos

Alta y edición conservan sus respuestas y reglas actuales. Las fechas y superficies inválidas se rechazan en el mismo orden y con los mismos errores. Las plantas omitidas en edición se conservan, una lista vacía las elimina y una lista provista se mapea igual que antes. Se preservan la referencia de la colección gestionada, transacciones, autorizaciones y esquema.

### Resumen y detalle de proyectos

Los dos formatos mantienen el contrato actual. El resumen omite la lectura de plantas, rubros, subcontratistas y contratos y devuelve esos campos nulos. El detalle usa listas vacías donde corresponda y ordena los rubros según su código. Ambos conservan el nombre histórico como respaldo cuando falta la empresa relacionada.

### Subcontratistas de proyecto

Alta y edición comparten la resolución de subcontratistas por empresa, tipo y selección. Se conservan el orden y mensaje de cada rechazo, la diferencia entre omitir y vaciar la selección, la selección implícita y el rechazo de quitar un subcontratista todavía asignado.

### Total de horas reales

La edición de proyecto conserva el valor manual cuando no hay registros. Si hay registros, usa la suma redondeada a dos decimales y un agregado nulo equivale a cero. Las mutaciones de registros mantienen el bloqueo del proyecto y sincronizan los cambios antes de calcular el total, incluido el cero al borrar el último registro.

## Arquitectura Controller → Service → Repository

Organizar las operaciones relacionadas en servicios por módulo. Los controllers mantienen rutas, validación HTTP y permisos existentes. Los servicios coordinan reglas y transacciones y usan los repositorios actuales. Los auxiliares compartidos se mantienen junto al módulo que los usa y se reutilizan si tienen el mismo contrato.

El primer corte cubre alta, edición y baja de registros horarios. Conserva rutas, DTO, códigos de respuesta, guardas, bloqueo de proyecto, orden de persistencia y cálculo de totales.

### Organización de paquetes de aplicación

Los servicios de aplicación quedan en `application.service`, los mappers en `application.mapper` y las reglas y cálculos auxiliares en `application.helper`. Se conservan los contratos, la lógica, las anotaciones Spring y los límites transaccionales.

### Nombres y reglas de dominio

Los componentes de aplicación llevan nombres de servicio según su responsabilidad. La jerarquía de rubros reside en `domain.rule` y conserva su comportamiento. Los puertos mantienen los contratos con infraestructura. Se preservan las rutas, DTO, permisos y transacciones.

### Retiro de funcionalidades demo

El backend arranca sin generar empresas, usuarios, proyectos, avances ni gastos demo. Se retiran el reset demo y el módulo global de gastos fijos, junto con su pantalla y cliente API del frontend si se aprueba el corte coordinado. Las pruebas conservan la cobertura de funcionalidades desplegadas y preparan sus propios datos en contextos efímeros. Se preservan la configuración de planes, el alta administrativa autorizada, el importador Excel, la protección del entorno de pruebas y los datos históricos.

### Retiro de Telegram y del alta por chat

Se retiran la vinculación de Telegram, la recepción de eventos y el alta de restricciones desde el chat. El asistente web conserva las consultas de restricciones y estado de obra. El alta y la edición regulares de restricciones conservan sus rutas, permisos y validaciones. Se conservan las restricciones existentes y la compatibilidad con respaldos en el primer corte.

### Controllers y pagos

Los controllers HTTP quedan en `controller` y los tres de pagos y suscripciones en `controller.pagos`. Los servicios de pagos y suscripciones se agrupan en `service.pagos`. La integración con el SDK conserva su ubicación en infraestructura y el puerto conserva su contrato. Se preservan rutas, acceso, respuestas y verificación de firma sobre el cuerpo original del webhook.
