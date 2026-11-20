CREATE TABLE vinculos_telegram (
    usuario_id BIGINT PRIMARY KEY REFERENCES usuarios_empresa(id) ON DELETE CASCADE,
    codigo_hash VARCHAR(64) UNIQUE,
    expira_en TIMESTAMP WITH TIME ZONE,
    chat_pendiente BIGINT,
    chat_confirmado BIGINT UNIQUE
);
