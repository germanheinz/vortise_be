-- Fase 2: Integración con Clerk
-- Agregar columnas necesarias para sincronización de usuarios

ALTER TABLE usuarios_empresa ADD COLUMN clerk_user_id VARCHAR(255) UNIQUE;
ALTER TABLE usuarios_empresa ADD COLUMN tiene_login BOOLEAN DEFAULT FALSE;
ALTER TABLE usuarios_empresa ADD COLUMN codigo_invitacion VARCHAR(255) UNIQUE;
ALTER TABLE usuarios_empresa ADD COLUMN invitacion_expira_en TIMESTAMP;
ALTER TABLE usuarios_empresa ADD COLUMN migrado_a_clerk BOOLEAN DEFAULT FALSE;

-- Índices para búsquedas rápidas
CREATE INDEX idx_clerk_user_id ON usuarios_empresa(clerk_user_id);
CREATE INDEX idx_codigo_invitacion ON usuarios_empresa(codigo_invitacion);
CREATE INDEX idx_migrado_a_clerk ON usuarios_empresa(migrado_a_clerk);
CREATE INDEX idx_tiene_login ON usuarios_empresa(tiene_login);
