# 016 — Calendario laboral por obra (sábados, domingos, recargos)

## Estado
**BORRADOR APROBADO.** Fase 1 implementada (configuración y cronograma); Fase 2 (costos) pendiente. Decisiones D-1 y D-2 aprobadas; D-3 aplica a la Fase 2. Repos afectados: `gestion_be` (modelo, migración, API, costos) y `gestion_fe` (formulario de obra, cronograma, costos). Relacionada con [010](../010-cronograma-laboral/spec.md).

## Problema
- El calendario laboral es por empresa (`calendarios_laborales_empresa`). La obra no puede decidir si trabaja sábados o domingos.
- Los campos `pagaDoble*` de la empresa se guardan, pero **ningún costo los usa**: hoy el costo es `HH × tarifa` fija (`PrevistoRealCalculator`, `GetEmpresaDashboardObrasUseCase`, `CronogramaSection`, `reportes-obra`).
- Una hora de sábado, domingo o feriado no vale lo mismo que una de día hábil y eso debe impactar en el cronograma y en lo económico.

## Comportamiento
### Configuración por obra
Cuatro campos nuevos en la obra, con valores iniciales tomados del calendario de la empresa:
| Campo | Significado | Valor inicial |
|---|---|---|
| `trabajaSabado` | La obra trabaja los sábados | `empresa.horasSabado > 0` |
| `trabajaDomingo` | La obra trabaja los domingos | `empresa.trabajaDomingo` |
| `pagaDobleFeriado` | Hora trabajada en feriado se paga doble | `empresa.pagaDobleFeriado` |
| `pagaDobleNoLaborable` | Hora cargada en un día que la obra declara no laborable (sábado/domingo sin check) se paga doble | `empresa.pagaDobleNoLaborable` |

- Las horas por día (L-V y sábado) y los feriados nacionales siguen viniendo de la empresa/código actual. Domingo usa las horas de L-V (comportamiento actual).
- Se muestran y editan solo al **crear** y al **editar** la obra. El cronograma no los muestra ni los edita: solo los usa para calcular.
- La edición en la empresa (Configuración) solo cambia los valores iniciales de obras nuevas; no modifica obras existentes.

### Cronograma
- El cálculo de fin y días usa el calendario efectivo de la obra: sábado cuenta solo si `trabajaSabado` (con las horas de sábado de la empresa); domingo solo si `trabajaDomingo`; feriados nacionales nunca son hábiles.
- Si `trabajaSabado` está marcado y las horas de sábado de la empresa son 0, el sábado no suma horas; la pantalla muestra una advertencia que indica dónde configurarlas (no se bloquea el guardado ni se asume un valor).
- Al cambiar el calendario en la edición de la obra, el cronograma calcula el fin automático con el calendario nuevo al mostrarlo; el fin guardado se actualiza la próxima vez que se guarda el cronograma. Los fines manuales no cambian.
- Días laborables estimados: ya implementado: exacto para cálculo, redondeado hacia arriba al mostrar.

### Costos
Factor de cada día de trabajo (se aplica a horas oficiales y ayudantes):
| Día | Factor |
|---|---|
| Lunes a viernes no feriado | 1 |
| Sábado trabajado (`trabajaSabado`) | 2 *(a confirmar, ver D-1)* |
| Domingo trabajado (`trabajaDomingo`) | 2 *(a confirmar, ver D-1)* |
| Feriado trabajado | 2 si `pagaDobleFeriado`, si no 1 |
| Sábado/domingo no laborable de la obra con horas cargadas | 2 si `pagaDobleNoLaborable`, si no 1 |

- **Costo previsto:** las HH previstas se reparten día a día según el cronograma (capacidad diaria = personas × horas del día × frentes) y cada día pesa por su factor.
- **Costo real:** las HH de cada carga (`ObraTarea`) se reparten uniformemente en sus días (`fecha` y `dias`, como en `diasDeCarga`) y cada día pesa por su factor.
- Se introduce el concepto de **horas ponderadas** = Σ horas del día × factor. Todo costo pasa a ser `horas ponderadas × tarifa`. Las HH, la productividad y los indicadores de producción **no cambian**: solo cambian los montos (costo, desvío, proyección).
- Sin tarifas válidas se mantiene el comportamiento actual (sin dato; nunca un 0 falso).

## Contratos (a documentar en `plan.md`)
- `ProyectoDto`/request de crear y editar: `trabajaSabado`, `trabajaDomingo`, `pagaDobleFeriado`, `pagaDobleNoLaborable` (opcionales al crear: si faltan se toman de la empresa).
- `PrevistoRealDto`: agrega horas ponderadas por actividad y por carga para el costo real.
- Autorización: la misma que editar la obra; no se expone el calendario de otra empresa.

## Fuera de alcance
- Edición de la lista de feriados y horas por día por obra.
- "Paga doble sábado" independiente del check de trabajo (ver D-1).
- Recalcular o reescribir datos históricos guardados: no se guardan costos, se calculan al leer.

## Decisiones a confirmar
- **D-1.** El check "Trabaja sábados" implica pago doble, y el domingo trabajado también (lo dicho: "siempre se paga doble"). El campo `pagaDobleSabado` de la empresa queda sin uso para esto. Alternativa: respetar `pagaDobleSabado` de la empresa.
- **D-2.** "Día no laborable" = sábado o domingo que la obra no trabaja pero que tiene horas cargadas. No hay una lista de días no laborables por fecha.
- **D-3.** El backend necesita la lista de feriados nacionales (hoy solo existe en `lib/cronograma-laboral.ts`); se porta a Java con las mismas reglas y pruebas gemelas.

## Aceptación
1. Crear y editar obra muestran los 4 campos con los valores de la empresa; guardar y reabrir los conserva.
2. Al cambiar los campos en la edición de la obra, el cronograma calcula el fin automático con ese calendario.
3. Obra sin sábados: ningún día sábado cuenta en el cronograma. Con sábados: cuenta con las horas de sábado de la empresa.
4. Una carga que cubre sábado y día hábil produce horas ponderadas = hábil×1 + sábado×2 (si D-1 se aprueba) y el costo real lo refleja.
5. Costo previsto con la misma obra coincide con el reparto del cronograma.
6. Obras existentes quedan con los valores derivados de su empresa y sus costos solo cambian si esa configuración implica recargo.
7. Sin tarifas: sin dato, sin cambios en HH ni indicadores.
8. Un usuario sin permiso de edición de la obra no puede cambiar el calendario por API.
