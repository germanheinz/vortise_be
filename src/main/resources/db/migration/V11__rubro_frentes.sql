ALTER TABLE rubros_obra
    ADD COLUMN frentes INTEGER DEFAULT 1 NOT NULL CHECK (frentes >= 1);