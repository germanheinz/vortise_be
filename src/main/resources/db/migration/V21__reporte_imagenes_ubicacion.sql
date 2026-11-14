-- Additive and nullable: existing images keep working without date, rubro or nivel.
ALTER TABLE reporte_imagenes ADD COLUMN fecha DATE;
ALTER TABLE reporte_imagenes ADD COLUMN rubro_id BIGINT;
ALTER TABLE reporte_imagenes ADD COLUMN rubro_nombre VARCHAR(255);
ALTER TABLE reporte_imagenes ADD COLUMN nivel_id BIGINT;
ALTER TABLE reporte_imagenes ADD COLUMN nivel_nombre VARCHAR(255);
