-- Días que abarca una carga diaria, contados hacia atrás desde su fecha. Aditiva: las cargas existentes quedan en 1 día.
ALTER TABLE obra_tareas ADD COLUMN dias INTEGER NOT NULL DEFAULT 1;
