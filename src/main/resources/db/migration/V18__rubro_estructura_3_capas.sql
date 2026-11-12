-- Estructura de actividades en 3 capas (rubro -> nivel/subrubro -> subrubro/nivel).
-- Aditiva: no elimina columnas legacy (rubro, nivel, sub_numero_rubro, sub_rubro). Conserva los ids de rubros_obra,
-- por lo que tareas, restricciones y certificados siguen enlazados. SQL portable entre H2 y PostgreSQL
-- (sin expresiones regulares ni funciones de ventana).

ALTER TABLE rubros_obra ADD COLUMN parent_id BIGINT;
ALTER TABLE rubros_obra ADD COLUMN tipo_nodo VARCHAR(16) DEFAULT 'SUBRUBRO' NOT NULL
    CHECK (tipo_nodo IN ('RUBRO', 'NIVEL', 'SUBRUBRO'));
ALTER TABLE rubros_obra ADD COLUMN variante_estructura VARCHAR(32)
    CHECK (variante_estructura IS NULL OR variante_estructura IN ('RUBRO_SUBRUBRO', 'RUBRO_NIVEL_SUBRUBRO', 'RUBRO_SUBRUBRO_NIVEL'));
ALTER TABLE rubros_obra ADD COLUMN nombre VARCHAR(255);
-- Columna temporal para reescribir los códigos sin que los cálculos lean códigos ya reescritos.
ALTER TABLE rubros_obra ADD COLUMN n_rubro_nuevo VARCHAR(255);

-- Si se elimina un nodo junto con su rama, el orden de borrado no debe violar la FK.
ALTER TABLE rubros_obra ADD CONSTRAINT fk_rubros_obra_parent
    FOREIGN KEY (parent_id) REFERENCES rubros_obra (id) ON DELETE SET NULL;
CREATE INDEX ix_rubros_obra_proyecto_parent ON rubros_obra (proyecto_id, parent_id);

-- Raíces: códigos numéricos múltiplos de 100 (de 3 a 9 dígitos), como deducía la aritmética "% 100" anterior.
UPDATE rubros_obra
   SET tipo_nodo = 'RUBRO', variante_estructura = 'RUBRO_SUBRUBRO'
 WHERE n_rubro IS NOT NULL
   AND LENGTH(n_rubro) BETWEEN 3 AND 9
   AND SUBSTRING(n_rubro, LENGTH(n_rubro) - 1, 2) = '00'
   AND REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
       n_rubro, '0', ''), '1', ''), '2', ''), '3', ''), '4', ''), '5', ''), '6', ''), '7', ''), '8', ''), '9', '') = '';

-- Resto de códigos numéricos: SUBRUBRO hoja con padre = código / 100 * 100 en la misma obra.
-- Los huérfanos (sin raíz coincidente) quedan como SUBRUBRO sin padre.
UPDATE rubros_obra c
   SET parent_id = (SELECT MIN(p.id)
                      FROM rubros_obra p
                     WHERE p.proyecto_id = c.proyecto_id
                       AND p.tipo_nodo = 'RUBRO'
                       AND p.n_rubro = SUBSTRING(c.n_rubro, 1, LENGTH(c.n_rubro) - 2) || '00')
 WHERE c.tipo_nodo = 'SUBRUBRO'
   AND c.n_rubro IS NOT NULL
   AND LENGTH(c.n_rubro) BETWEEN 3 AND 9
   AND REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
       c.n_rubro, '0', ''), '1', ''), '2', ''), '3', ''), '4', ''), '5', ''), '6', ''), '7', ''), '8', ''), '9', '') = '';

-- Nombre: rubro para las raíces, y subrubro (o rubro si falta) para el resto.
UPDATE rubros_obra SET nombre = rubro WHERE tipo_nodo = 'RUBRO';
UPDATE rubros_obra SET nombre = COALESCE(NULLIF(TRIM(sub_rubro), ''), rubro) WHERE tipo_nodo <> 'RUBRO';

-- Códigos jerárquicos: la raíz toma su posición ascendente en la obra (5000 -> 1) y cada hijo
-- la posición ascendente entre los hijos de su raíz (5001 -> 1.1). Los códigos son cadenas de dígitos,
-- por lo que se comparan por (longitud, texto) sin convertirlos a número.
UPDATE rubros_obra r
   SET n_rubro_nuevo = CAST((SELECT COUNT(*) + 1
                               FROM rubros_obra o
                              WHERE o.proyecto_id = r.proyecto_id
                                AND o.tipo_nodo = 'RUBRO'
                                AND (LENGTH(o.n_rubro) < LENGTH(r.n_rubro)
                                     OR (LENGTH(o.n_rubro) = LENGTH(r.n_rubro) AND o.n_rubro < r.n_rubro)
                                     OR (o.n_rubro = r.n_rubro AND o.id < r.id))) AS VARCHAR(20))
 WHERE r.tipo_nodo = 'RUBRO';

UPDATE rubros_obra c
   SET n_rubro_nuevo = (SELECT p.n_rubro_nuevo FROM rubros_obra p WHERE p.id = c.parent_id) || '.'
       || CAST((SELECT COUNT(*) + 1
                  FROM rubros_obra s
                 WHERE s.parent_id = c.parent_id
                   AND (LENGTH(s.n_rubro) < LENGTH(c.n_rubro)
                        OR (LENGTH(s.n_rubro) = LENGTH(c.n_rubro) AND s.n_rubro < c.n_rubro)
                        OR (s.n_rubro = c.n_rubro AND s.id < c.id))) AS VARCHAR(20))
 WHERE c.parent_id IS NOT NULL;

-- Horas registradas enlazan por texto: se reescribe el código antes de reemplazar n_rubro.
UPDATE registros_horas
   SET numero_rubro = (SELECT MIN(r.n_rubro_nuevo)
                         FROM rubros_obra r
                        WHERE r.proyecto_id = registros_horas.proyecto_id
                          AND r.n_rubro = registros_horas.numero_rubro
                          AND r.n_rubro_nuevo IS NOT NULL)
 WHERE numero_rubro IS NOT NULL
   AND EXISTS (SELECT 1
                 FROM rubros_obra r
                WHERE r.proyecto_id = registros_horas.proyecto_id
                  AND r.n_rubro = registros_horas.numero_rubro
                  AND r.n_rubro_nuevo IS NOT NULL);

UPDATE rubros_obra SET n_rubro = n_rubro_nuevo WHERE n_rubro_nuevo IS NOT NULL;
ALTER TABLE rubros_obra DROP COLUMN n_rubro_nuevo;
