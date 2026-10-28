ALTER TABLE usuarios_empresa ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE usuarios_empresa ADD COLUMN persistence_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE password_reset_tokens (
    usuario_id BIGINT PRIMARY KEY REFERENCES usuarios_empresa(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    vence_en TIMESTAMP WITH TIME ZONE NOT NULL,
    consumido_en TIMESTAMP WITH TIME ZONE,
    session_version BIGINT NOT NULL
);
CREATE TABLE password_reset_quotas (
    clave VARCHAR(72) PRIMARY KEY,
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    solicitudes INTEGER NOT NULL
);
INSERT INTO password_reset_quotas (clave, inicio, solicitudes) VALUES ('global', CURRENT_TIMESTAMP, 0);
