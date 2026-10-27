ALTER TABLE pagos_suscripcion ADD COLUMN monto_devuelto_centavos BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pagos_suscripcion ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE devoluciones_pago (
    pago_id BIGINT NOT NULL REFERENCES pagos_suscripcion(id),
    stripe_refund_id VARCHAR(255) NOT NULL,
    monto_centavos BIGINT NOT NULL CHECK (monto_centavos >= 0),
    estado VARCHAR(255) NOT NULL,
    fecha TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (pago_id, stripe_refund_id),
    UNIQUE (stripe_refund_id)
);
