# Refactor de legibilidad y arquitectura del backend

El objetivo es que cada caso de uso exprese su flujo de negocio y que los cálculos y adaptadores tengan responsabilidades reconocibles. El piloto será Previsto vs. Real. Los demás módulos son candidatos a revisiones posteriores con criterios propios.

## Criterios de aceptación del piloto

- El endpoint de indicadores y el dashboard conservan sus resultados, campos, orden, valores nulos, errores y límites de acceso por empresa y obra. Se conserva la consulta transaccional de solo lectura y el contrato Java que usan sus consumidores.
- Se conservan la inclusión de hojas y rubros con cargas, el conteo de tareas sin vincular y el indicador de desencofrado. La dotación se resuelve con la precedencia del rubro, sus niveles intermedios y la raíz.
- Se conservan las fórmulas, el orden de las operaciones y el tratamiento de cero, nulos, valores negativos y valores no finitos. Se cubre la diferencia entre agregar cantidades y agregar horas. Los costos siguen requiriendo una única categoría habilitada con el nombre correspondiente.
- La línea base propuesta es el árbol de trabajo actual. En ese código, las horas previstas a fecha intentan usar días transcurridos desde el inicio planificado, luego días de la tarea y finalmente horas previstas del rubro. El campo de días sigue la asignación de cada rama, incluso si un cálculo devuelve un valor no finito. La fecha sigue dependiendo de la zona horaria del proceso. La línea base queda pendiente de confirmación con la aprobación del plan.
- La implementación debe permitir leer el recorrido principal sin recorrer las fórmulas. Los cálculos extraídos se pueden verificar con JUnit sin Spring ni base de datos. El alcance excluye cambios de esquema, dependencias, despliegues y frontend.

La especificación funcional existente sigue en [Previsto vs. Real](../002-previsto-real/spec.md).
