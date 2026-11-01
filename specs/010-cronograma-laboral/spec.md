# 010 — Plazo de pedidos y cronograma laboral

## Estado

Plan aprobado el 2026-11-01. Implementación fuente completada; tests, migración y despliegue no ejecutados. Seguir [AGENTS.md](../../AGENTS.md).

## Comportamiento propuesto

- El proyecto tiene `pedidosDeObraDias`, opcional, entero no negativo. En el paso Equipo se presenta como “Pedidos de obra”. Desde `fechaInicio`, la fecha límite inclusiva es `fechaInicio + pedidosDeObraDias` días corridos. Pasada esa fecha se rechazan nuevas restricciones en el backend. Si el plazo o la fecha de inicio no existen, no se limita el alta. Las restricciones existentes pueden editarse.
- `cantidadPersonas` ya existe en `RubroObra` y el contrato de rubros, pero no se captura en Cronograma. Exponerla como entero positivo por subrubro.
- Calcular duración a partir de HH previstas OF+AY, cantidad de personas, horas laborables por día y frentes. El fin automático consume capacidad diaria del calendario de empresa y omite días no laborables y feriados nacionales oficiales de Argentina. Sábado es laborable si las horas configuradas son mayores a cero; domingo depende de `trabajaDomingo` y usa horas L-V. Esta propuesta no excluye días turísticos/puente.
- Inicio permanece editable. Fin automático se bloquea; `cronogramaManual` lo habilita. Al desactivar el ajuste manual, recalcular el fin. El rango guardado es el que actualiza la barra.
- El backend devuelve rubros ordenados por código numérico ascendente para que Estructura de actividades y Cronograma coincidan, independientemente de orden de fechas.
- Retirar inputs de superficies/coefs por nivel en crear/editar sin borrar los niveles ni alterar esos valores existentes.

## Persistencia y contratos

- Agregar `pedidos_de_obra_dias` nullable a `proyectos`; actualizar `Proyecto`, DTO de create/update/detail, mapper y use cases.
- Restricción central en `CreateRestriccionUseCase`; mantener las APIs de edición de restricciones sin limitación de fecha.
- `cantidadPersonas`, `frentes`, HH y fechas ya existen en el contrato/esquema de rubros; no agregar columnas para esos datos.
- Migración Flyway aditiva V15 y `schema.sql` local idempotente para el plazo de pedidos. No aplicar migraciones durante esta tarea ni tocar datos persistentes.
- Feriados se mantienen en el código como datos locales por año; no depender de una API remota. Requiere actualización anual y pruebas para los años incluidos.

## Aceptación propuesta

1. Crear/editar obra guarda el plazo entero y conserva restricciones de permisos/tenant.
2. Restricciones se pueden crear hasta el día límite inclusive y se rechazan después; faltando fecha de inicio/plazo no se bloquea; actualizar una existente sigue permitido.
3. Cronograma captura cantidad de personas, calcula el fin por capacidad de jornada/frentes, respeta días hábiles y feriados, y bloquea fin salvo ajuste manual.
4. Cambio a manual deja editar fin y actualiza la barra; volver a automático recalcula el fin.
5. Estructura y Cronograma muestran la misma secuencia ascendente de códigos.
6. Retirar controles por nivel no borra los valores persistidos existentes.
7. Casos multiempresa, fecha límite, entradas enteras, cronología, actualización y compatibilidad quedan cubiertos por pruebas.
