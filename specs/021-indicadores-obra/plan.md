# 021 — Plan técnico backend

Requisitos en [spec](spec.md). Estado y evidencia en [tasks](tasks.md). Consumidor: [plan frontend 042](../../../gestion_fe/specs/042-indicadores-obra/plan.md).

## Situación actual (descubrimiento)

| Pieza | Problema frente a la spec |
|---|---|
| `domain/rule/PrevistoRealCalculator` | Usa el modelo de la planilla: producción = cantidad − cantidad equivalente, recursos = (HG − HR) ÷ h y umbrales 1,0/1,2 y ±1. Solo el desvío y la proyección coinciden. |
| `domain/rule/CalculoAnexoPrevistoReal.calcular` | Si hay fecha de inicio, calcula HF con los días corridos hasta hoy en lugar de los días de la carga, y lo repite en cada carga, de modo que la suma lo multiplica. Sin personas, cae al total de HH previstas en vez de `null`. |
| `service/proyecto/PrevistoRealService` | Solo llega al nivel de subrubro. Piso, rubro y obra los arma el frontend con dos cuentas distintas. |
| `service/empresa/EmpresaConsultaService.obra` | El % proyectado es `proyección ÷ costo`, cuando la spec pide `(desvío + proyección) ÷ presupuesto`. Tampoco incluye la proyección de los subrubros sin avance. |

## Diseño

### Dominio puro: `domain/rule/indicadores/`

Clases sin Spring ni repositorios.

- `Estado`: `clasificar(indice)` aplica 0,9 y 1,1 inclusive y devuelve `BAJO`, `ESTABLE` o `ALTO`. Cada nivel lo traduce a sus etiquetas (Lenta/Estable/Rápida, etc.).
- `Redondeo.alejandoDelCero(double)` con `BigDecimal` y `RoundingMode.HALF_UP`.
- `Magnitudes` (record sumable): `hf`, `hg`, `hr`, `costoPrevistoHecho`, `costoReal`, `presupuesto`, `proyeccion`.
  - `sumar(Magnitudes)` trata `null` como "sin dato" y no como 0.
  - Las razones devuelven `null` si el denominador es ≤ 0 o el resultado no es finito.
- `Indices` (record): producción, productividad y recursos, con sus estados.
- `CalculadoraIndicadores`, con funciones puras para cada nivel:
  - `presupuesto(subrubro, parametros)`: CI-01.
  - `tarea(carga, subrubro, parametros)`: CI-02, incluidas personas/día.
  - `subrubro(cargas, …)`: CI-03. Suma y después divide, y calcula jornales y proyección.
  - `referencias(subrubrosDeLaObra)`: CI-04. Agrupa por (rubro raíz, nombre normalizado, unidad normalizada) y por piso, y excluye el piso propio.
  - `agrupar(subrubros)` para piso y rubro: CI-05 y CI-06.
  - `obra(rubros)`: CI-07, con las dos proyecciones y los % de pagado y de obra hecha.

### Servicio y API

- `service/proyecto/IndicadoresObraService` arma las entradas con los repositorios existentes (rubros, cargas, categorías y calendario) y llama al dominio. Es de solo lectura.
- `GET /api/proyectos/{proyectoId}/indicadores` en `ObraTareaController`, con las mismas guardas de lectura y de tenant que el endpoint actual de indicadores.
- DTO `application/dto/IndicadoresObraDto`, con esta forma:

```
IndicadoresObraDto {
  parametros { costoHoraOficial, costoHoraAyudante, horasJornada }
  tareasSinVincular
  obra { magnitudes, desvio, desvioPorcentaje,
         proyeccionIniciada, porcentajeIniciada,          // CI-07 "lo que ya arrancó"
         proyeccionSinAvance, proyeccionTotal, porcentajeTotal,
         porcentajePagado, porcentajeHecho }
  rubros[] { rubroId, nombre, agregado,
             pisos[] { nivelId?, nombre?, agregado,
                       subrubros[] { rubroId, nombre, unidad, conAvance,
                                     presupuesto { cantidad, hhTotales, costo, precioUnitario, personas,
                                                   diasPrevistos, diasPrevistosRedondeados },
                                     cantidadEjecutada, dias, magnitudes, indices, jornales,
                                     desvio, desvioPorcentaje, proyeccion, referencia? { pisos[], porcentaje },
                                     tareas[] { tareaId, fecha, dias, cantidad, magnitudes, indices,
                                                personasPorDia?, personasPrevistas, desvio } } } } }
  agregado = { conAvance, magnitudes, indices, porcentajeHoras, desvio, desvioPorcentaje,
               proyeccion, porcentajeAlTerminar }
}
```

- Los porcentajes se entregan como fracciones (−0,6), no como valores ×100. El frontend multiplica y redondea.
- `EmpresaConsultaService.obra` pasa a usar `obra.desvio`, `obra.porcentaje*` y `obra.proyeccionTotal` del nuevo servicio. Los nombres de campo de `EmpresaDashboardObrasDto` no cambian.

### Retiro del modelo viejo (al final, cuando el frontend 042 ya migró)

- Se eliminan `PrevistoRealCalculator`, `CalculoAnexoPrevistoReal`, `PrevistoRealService`, el endpoint `GET /api/proyectos/{id}/obra-tareas/indicadores` y sus pruebas.
- Antes se confirma con grep que no quedan consumidores en `gestion_fe`, en los reportes ni en el asistente.

## Pruebas

- `CalculadoraIndicadoresTest`: sin Spring, con el conjunto de datos del documento. Comprueba cada número de CI-01 a CI-07 y estos casos:
  - las dos cargas equivalen a una;
  - la proyección es 0 cuando la cantidad ejecutada supera la prevista;
  - la división por 0 da `null`;
  - el redondeo alejándose del 0 (±0,5);
  - los límites de estado en 0,9 y 1,1 exactos;
  - los subcontratos quedan excluidos.
- `IndicadoresObraHttpTest`:
  - el contrato JSON del endpoint;
  - aislamiento entre empresas (403 o 404 sobre una obra ajena);
  - editar o borrar una carga cambia el resultado (CI-08.3).
- Se actualizan `EmpresaDashboardObrasUseCaseTest` y las pruebas de `EmpresaConsultaService` para los nuevos porcentajes.

## Orden

1. Dominio y pruebas puras.
2. Servicio, endpoint y prueba HTTP.
3. Dashboard.
4. Migración del frontend (042).
5. Retiro del modelo viejo.
6. `mvn verify`, verificación del frontend y revisión independiente.

## Riesgos

- **Cambio visible de números y estados:** es intencional, porque se reemplaza el modelo. Los reportes ya emitidos no se recalculan.
- **Costo de lectura:** se mantiene el mismo número de consultas que `PrevistoRealService` (rubros más cargas más categorías). El cálculo es en memoria.
- **Supuestos S1 a S5 de la spec:** si cambian, la corrección queda acotada a `CalculadoraIndicadores`.
