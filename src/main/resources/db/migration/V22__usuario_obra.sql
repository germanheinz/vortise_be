-- Obras asignadas a cada jefe de obra (spec 014). Aditiva: no modifica tablas existentes.
CREATE TABLE usuario_obra (
    usuario_id BIGINT NOT NULL REFERENCES usuarios_empresa(id) ON DELETE CASCADE,
    proyecto_id BIGINT NOT NULL REFERENCES proyectos(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, proyecto_id)
);
CREATE INDEX idx_usuario_obra_proyecto ON usuario_obra(proyecto_id);
