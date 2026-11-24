-- Carga de horas de desencofrado: solo imputa horas, sin cantidad. Aditiva: las cargas existentes quedan en false.
ALTER TABLE obra_tareas ADD COLUMN desencofrado BOOLEAN NOT NULL DEFAULT FALSE;
