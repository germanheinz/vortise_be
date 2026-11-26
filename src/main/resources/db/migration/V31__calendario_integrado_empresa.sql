ALTER TABLE empresas ADD COLUMN calendario_id BIGINT UNIQUE;
ALTER TABLE empresas ADD COLUMN calendario_horas_lunesaviernes DOUBLE PRECISION;
ALTER TABLE empresas ADD COLUMN calendario_horas_sabado DOUBLE PRECISION;
ALTER TABLE empresas ADD COLUMN calendario_trabaja_domingo BOOLEAN;
ALTER TABLE empresas ADD COLUMN calendario_paga_doble_feriado BOOLEAN;
ALTER TABLE empresas ADD COLUMN calendario_paga_doble_sabado BOOLEAN;
ALTER TABLE empresas ADD COLUMN calendario_paga_doble_no_laborable BOOLEAN;

UPDATE empresas
SET calendario_id = c.id,
    calendario_horas_lunesaviernes = c.horas_lunesaviernes,
    calendario_horas_sabado = c.horas_sabado,
    calendario_trabaja_domingo = c.trabaja_domingo,
    calendario_paga_doble_feriado = c.paga_doble_feriado,
    calendario_paga_doble_sabado = c.paga_doble_sabado,
    calendario_paga_doble_no_laborable = c.paga_doble_no_laborable
FROM calendarios_laborales_empresa c
WHERE empresas.id = c.empresa_id;

-- Keep the old rows during the transition and preserve the old company-delete behavior.
ALTER TABLE calendarios_laborales_empresa DROP CONSTRAINT fk_calendarios_laborales_empresa_empresa;
ALTER TABLE calendarios_laborales_empresa
    ADD CONSTRAINT fk_calendarios_laborales_empresa_empresa
    FOREIGN KEY (empresa_id) REFERENCES empresas (id) ON DELETE CASCADE;
