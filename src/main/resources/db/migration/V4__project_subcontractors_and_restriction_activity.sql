CREATE TABLE proyecto_subcontratistas (
    proyecto_id BIGINT NOT NULL,
    catalogo_empresa_id BIGINT NOT NULL,
    CONSTRAINT pk_proyecto_subcontratistas PRIMARY KEY (proyecto_id, catalogo_empresa_id),
    CONSTRAINT fk_proyecto_subcontratistas_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos (id),
    CONSTRAINT fk_proyecto_subcontratistas_catalogo FOREIGN KEY (catalogo_empresa_id) REFERENCES catalogos_empresa (id)
);
CREATE INDEX ix_proyecto_subcontratistas_catalogo ON proyecto_subcontratistas (catalogo_empresa_id);

ALTER TABLE rubros_obra ADD CONSTRAINT uq_rubros_obra_proyecto_id_id UNIQUE (proyecto_id, id);

ALTER TABLE restricciones_obra ADD COLUMN rubro_id BIGINT;
ALTER TABLE restricciones_obra ADD CONSTRAINT fk_restricciones_obra_rubro
    FOREIGN KEY (proyecto_id, rubro_id) REFERENCES rubros_obra (proyecto_id, id);
CREATE INDEX ix_restricciones_obra_rubro ON restricciones_obra (rubro_id);