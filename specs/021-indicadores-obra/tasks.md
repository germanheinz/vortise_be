# 021 — Estado y evidencia

- [x] Descubrimiento: se compararon los tres cálculos actuales (`PrevistoRealCalculator` en el backend; `indicadores-anexo.ts` y `seguimiento-a-la-fecha.ts` en el frontend; `reportes-obra.ts` e `indicadores-nivel.ts` en el frontend) con CI-01 a CI-08. Ver [plan](plan.md#situación-actual-descubrimiento).
- [x] La aritmética del documento se verificó con el conjunto de datos y cierra en todos los criterios.
- [x] Decisiones: el backend es la única fuente, el modelo viejo se reemplaza y personas/día se redondea alejándose del 0.
- [x] Spec, plan y supuestos S1 a S5 aprobados (2026-11-26).
- [x] Ejecución autorizada: `mvn -Dtest=... test`, `mvn verify` y prueba HTTP con H2 en memoria. Sin tocar `./data` ni hacer commits.
- [x] Dominio `domain/rule/indicadores/{CalculadoraIndicadores, Estado, Redondeo}`.
  - `CalculadoraIndicadoresTest`: 16/16, con el conjunto de datos del documento. Cubre CI-01 a CI-07, las dos cargas equivalentes a una, la proyección 0, el ±0,5, los límites 0,9/1,1, `null` sin personas o sin tarifas y el desencofrado.
  - Los estados se clasifican sobre el índice redondeado a 2 decimales, así el estado coincide con el número que se muestra (1,1000000000000003 → Estable).
- [x] `IndicadoresObraService` y `GET /api/proyectos/{id}/indicadores` (`IndicadoresObraController`). El DTO aplana las magnitudes en cada nivel en lugar de anidarlas en `magnitudes`.
  - `IndicadoresObraServiceTest`: 3/3. Cubre las variantes rubro → nivel → subrubro y rubro → subrubro → nivel, personas × frentes, cargas fuera de una hoja contadas como "sin vincular", exclusión de subcontratos y rubro sin pisos.
  - `IndicadoresObraHttpTest`: 3/3. Cubre el contrato con el conjunto de datos, el recálculo al editar y al borrar, 401 y 403 sobre una obra ajena.
- [x] El dashboard de empresa usa `IndicadoresObraService`: desvío actual con % sobre lo hecho, y desvío proyectado = proyección total con % al terminar. `EmpresaDashboardObrasUseCaseTest` 4/4, reescrito sobre el DTO nuevo.
- [x] `mvn clean verify` con el modelo nuevo agregado: 729 tests, 0 fallos, 0 errores, 29 omitidos (los mismos que en la línea base). El modelo viejo todavía convive.
- [ ] Retiro del modelo viejo, después de 042. **Decisión del usuario:** se hace en un corte siguiente, una vez desplegados el backend y el frontend. Mientras tanto el endpoint viejo convive y no tiene consumidores en `gestion_fe`.
- [ ] Revisión independiente: pospuesta por el usuario.
- [ ] `mvn verify` y revisión independiente.
