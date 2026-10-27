ALTER TABLE pagos_suscripcion ADD COLUMN intentos BIGINT;
ALTER TABLE pagos_suscripcion ADD COLUMN proximo_intento TIMESTAMP WITH TIME ZONE;
ALTER TABLE pagos_suscripcion ADD COLUMN estado_factura VARCHAR(255);
ALTER TABLE pagos_suscripcion ADD COLUMN fecha_pago TIMESTAMP WITH TIME ZONE;
ALTER TABLE pagos_suscripcion ADD COLUMN periodo_inicio TIMESTAMP WITH TIME ZONE;
ALTER TABLE pagos_suscripcion ADD COLUMN periodo_fin TIMESTAMP WITH TIME ZONE;
