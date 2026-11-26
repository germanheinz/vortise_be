# 021 — Indicadores de obra (backend)

## Estado

Borrador para aprobación (2026-11-26). Fuente: Google Doc "Cálculo de indicadores" (05-TEST) y prototipo. Consumidor: [gestion_fe 042](../../../gestion_fe/specs/042-indicadores-obra/spec.md).

Decisiones del usuario:

- El backend es la única fuente de cálculo para los niveles tarea, subrubro, piso, rubro y obra. El frontend solo da formato.
- El modelo "PREV VS REAL.xlsx" (`PrevistoRealCalculator`, con producción en unidades, umbrales 1,0/1,2 y recursos ±1 jornal) se reemplaza en todas las pantallas, reportes y en el dashboard.
- Personas/día se redondea alejándose del 0, con signo, y su magnitud mínima es 1 cuando el índice de recursos está fuera de 0,9–1,1.

## Definiciones

- **Datos del cálculo:**
  - costo hora de oficial y de ayudante: categorías de la empresa;
  - `h`: horas de lunes a viernes del calendario de la empresa;
  - `P`: personas del Cronograma (dotación del subrubro, heredada del nivel o del rubro).
- **Presupuesto del subrubro:**
  - HH totales = cantidad × (OF + AY);
  - costo = cantidad × (OF × $OF + AY × $AY);
  - precio/u = OF × $OF + AY × $AY;
  - días previstos = HH ÷ (P × h), y se muestra redondeado hacia arriba.
- **Siglas:**
  - **HF** (HH previstas a la fecha) = P × h × días de la carga. No depende de la fecha de hoy ni de la fecha de inicio.
  - **HG** (HH de lo hecho) = cantidad × HH/u.
  - **HR** (HH reales) = HH OF + HH AY.
- **Índices:**
  - producción = HG ÷ HF;
  - productividad = HG ÷ HR;
  - recursos = HR ÷ HF.
- **Estados:**
  - menos de 0,9: Lenta / Baja / Insuficiente;
  - de 0,9 a 1,1, ambos incluidos: Estable / Estable / Suficiente;
  - más de 1,1: Rápida / Alta / Excedido.
- **Economía:**
  - costo previsto de lo hecho = cantidad × precio/u;
  - costo real = HH OF × $OF + HH AY × $AY;
  - desvío = costo previsto de lo hecho − costo real.
- **Solo mano de obra propia:** los subrubros subcontratados no suman en ningún nivel.

## Criterios de aceptación

El conjunto de datos de prueba del documento es el fixture de todas las pruebas: costo HH oficial $5.000, ayudante $4.000, 8 h por día, platea con 2 personas y una carga en Platea de Subsuelo 2 de 3 días, 40 + 40 HH y 10 m³.

### CI-01 · Presupuesto

1. HH totales de la platea = 100.
2. Costo de la platea = 450.000.
3. Días previstos = 6,25; se muestra 7.
4. Personas del Cronograma. Su edición en Rendimientos es de solo lectura (frontend, 042).

### CI-02 · Tarea

1. HF = 48, HG = 50 y HR = 80.
2. Producción = 1,0417 (Estable, +4%).
3. Productividad = 0,625 (Baja; se muestra 0,63).
4. Recursos = 1,6667 (Excedido, +67%).
5. Personas/día = HR ÷ (d × h) − P = 1,33 → +1.
   - Con el índice de recursos entre 0,9 y 1,1, en lugar de un número se indica "personas previstas".
6. Desvío = −135.000.

### CI-03 · Subrubro

1. Se suman días, HF, HG, HR, cantidad y costos de todas las cargas, y recién después se divide.
2. Dos cargas (2 d, 25 + 25, 6 m³ y 1 d, 15 + 15, 4 m³) dan exactamente el mismo resultado que una sola de 3 d, 40 + 40, 10 m³.
3. Producción Estable +4% y productividad Baja 0,63.
4. Recursos Excedido 1,67, con jornales = (HR − HF) ÷ h = +4.
   - Es un entero redondeado alejándose del 0: 0,5 → 1 y −0,5 → −1.
5. Desvío = −135.000.
6. Proyección = desvío ÷ cantidad ejecutada × (cantidad prevista − ejecutada) = −135.000.
7. Si la cantidad ejecutada supera la prevista, la proyección es 0.

### CI-04 · Subrubro sin avance

1. Referencia: subrubros del mismo rubro raíz, con el mismo nombre y unidad (normalizados) y con avance en otro piso.
2. % = Σ desvío ÷ Σ costo previsto de lo hecho de las referencias = −60%.
3. Proyección = % × presupuesto del subrubro. Platea de Subsuelo 1: −270.000, con la referencia "Subsuelo 2" y −60%.
4. Sin referencia: proyección 0 y marca de "sin referencia".
5. Piso Subsuelo 1: resultado al terminar −270.000, −24% del presupuesto del piso.

### CI-05 · Piso

1. HF, HG, HR, costo previsto de lo hecho y costo real se suman solo de los subrubros con avance. Proyección y presupuesto se suman de todos.
2. No se suman cantidades ni días.
3. Producción: se expone el índice y el estado (el frontend muestra solo el estado). Subsuelo 2 = Estable.
4. Productividad Baja 0,63; recursos Excedido 1,67, +67% de horas.
5. Desvío −135.000, −60% sobre lo hecho.
6. Proyección −135.000. % al terminar = (desvío + proyección) ÷ presupuesto del piso = −27%.
7. Un piso sin subrubros con avance no tiene índices ("Sin avance") y expone solo la proyección.

### CI-06 · Rubro

1. Misma cuenta que el piso, sumando todos sus pisos.
2. Presupuesto = 999.000 + 1.134.000 + 360.000 = 2.493.000.
3. Índices iguales a los de Subsuelo 2. Desvío −135.000, −60%.
4. Proyección = −135.000 − 270.000 + 0 = −405.000; −22% al terminar.

### CI-07 · Obra

1. Solo mano de obra propia.
2. Costo previsto de lo hecho 225.000, costo real 360.000 y presupuesto 2.493.000.
3. Pagado de más 135.000. Pagado sobre presupuesto: 360.000 ÷ 2.493.000 = 14%. Obra hecha: 225.000 ÷ 2.493.000 = 9%.
4. Desvío −135.000, −60% sobre lo hecho.
5. Proyección de lo que ya arrancó = −135.000, −11% = (−135.000 − 135.000) ÷ 2.493.000.
6. Proyección si los pisos siguientes rinden igual = −405.000, −22%. Se informa además la suma de los subrubros sin avance: −270.000.
7. El dashboard de empresa usa estos mismos valores.

### CI-08 · Generales

1. Nunca se promedian índices ni porcentajes: siempre se suma y después se divide. Tampoco se calcula un estado "más frecuente".
2. División por 0 → `null` (el frontend muestra "Sin datos"). Nunca NaN ni Infinity.
3. Los indicadores se calculan al leer, así que editar o borrar una carga recalcula todos los niveles.
4. El backend entrega valores sin redondear, salvo los enteros definidos aquí (días previstos redondeados, personas/día y jornales).

## Supuestos a confirmar

- **S1. Frentes:** `P` = personas × frentes, como ya hace el plan del cronograma en el frontend. El fixture tiene 1 frente.
- **S2. Cargas sin días:** se toma 1 día, que es el valor por defecto actual de `ObraTarea.dias`.
- **S3. Cargas de desencofrado o sin cantidad:** suman HR pero no HG ni cantidad.
- **S4. Rubros sin pisos** (rubro → subrubro directo): forman un único grupo sin nombre de piso.
- **S5. Sin personas, sin jornada o sin tarifas:** los índices o la economía quedan en `null`. Ya no se usa como respaldo el total de HH previstas.

## Fuera de alcance

- Cambios en la carga de avance, el cronograma o los rendimientos, salvo exponer los datos.
- Indicadores de subcontratos.
- Migraciones de datos: no se persiste ningún derivado.
