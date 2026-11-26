# Simplificar el modelo de datos

La empresa concentra su configuración única. La obra conserva sus registros operativos consultables por separado. El frontend debe seguir recibiendo los mismos resultados para los mismos datos.

## Primera entrega

Integrar el calendario laboral en Empresa manteniendo el contrato HTTP, incluido el identificador devuelto por la API si corresponde, y el comportamiento cuando falta el calendario. Preservar defaults, horas de jornada, recargos y configuración propia de cada obra. Las altas, ediciones y bajas deben mantener el aislamiento por empresa.

## Entregas siguientes

- Medir las consultas que cargan listas completas de tareas y horas por obra. Preservar la navegación y los totales mientras se introducen filtros y paginación donde aporten valor.
- Comparar las escrituras del asistente de tareas y del calendario de horas. Consolidar campos únicamente cuando se compruebe que representan el mismo hecho.
- Confirmar consumidores de gastos fijos y de la invitación legada antes de proponer su retiro. Revisar el uso de las tablas históricas en los respaldos antes de decidir su destino.
