-- Refuse ambiguous existing data. Never delete or rename customer configuration.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM unidades_metricas_empresa GROUP BY empresa_id, codigo HAVING COUNT(*) > 1)
       OR EXISTS (SELECT 1 FROM categorias_mano_obra_empresa GROUP BY empresa_id, nombre HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Duplicate company configuration: review required before adding uniqueness constraints';
    END IF;
END $$;
ALTER TABLE unidades_metricas_empresa ADD CONSTRAINT uq_unidades_empresa_codigo UNIQUE (empresa_id, codigo);
ALTER TABLE categorias_mano_obra_empresa ADD CONSTRAINT uq_categorias_empresa_nombre UNIQUE (empresa_id, nombre);

-- A one-time marker preserves intentional deletions after initial preparation.
ALTER TABLE empresas ADD COLUMN configuracion_inicializada BOOLEAN NOT NULL DEFAULT FALSE;
