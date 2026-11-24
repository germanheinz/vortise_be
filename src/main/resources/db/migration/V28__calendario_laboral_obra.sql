-- Calendario laboral por obra (spec 016). Aditiva: las obras existentes toman los valores del calendario de su empresa,
-- de modo que el cronograma actual no cambia. Sin calendario de empresa se usan los valores por defecto del calendario (sábado de 4 h).
ALTER TABLE proyectos ADD COLUMN trabaja_sabado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE proyectos ADD COLUMN trabaja_domingo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE proyectos ADD COLUMN paga_doble_feriado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE proyectos ADD COLUMN paga_doble_no_laborable BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE proyectos SET
    trabaja_sabado = COALESCE((SELECT c.horas_sabado > 0 FROM calendarios_laborales_empresa c WHERE c.empresa_id = proyectos.empresa_id), TRUE),
    trabaja_domingo = COALESCE((SELECT c.trabaja_domingo FROM calendarios_laborales_empresa c WHERE c.empresa_id = proyectos.empresa_id), FALSE),
    paga_doble_feriado = COALESCE((SELECT c.paga_doble_feriado FROM calendarios_laborales_empresa c WHERE c.empresa_id = proyectos.empresa_id), FALSE),
    paga_doble_no_laborable = COALESCE((SELECT c.paga_doble_no_laborable FROM calendarios_laborales_empresa c WHERE c.empresa_id = proyectos.empresa_id), FALSE);
