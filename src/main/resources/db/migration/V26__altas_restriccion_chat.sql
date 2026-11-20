CREATE TABLE altas_restriccion_chat (
    clave_hash VARCHAR(64) PRIMARY KEY,
    solicitud_hash VARCHAR(64) NOT NULL,
    proyecto_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    restriccion_id BIGINT NOT NULL
);
