-- Añade la estimación opcional en talla de camiseta a work_item como columna
-- dedicada y validada por la base de datos (decisión de usuario 2026-09-27):
-- no como un valor dentro del JSON de campos por tipo (V1/V2 no lo modelan).

ALTER TABLE work_item ADD COLUMN tshirt_size VARCHAR(8);

ALTER TABLE work_item ADD CONSTRAINT ck_work_item_tshirt_size
    CHECK (tshirt_size IS NULL OR tshirt_size IN ('XS', 'S', 'M', 'L', 'XL'));
