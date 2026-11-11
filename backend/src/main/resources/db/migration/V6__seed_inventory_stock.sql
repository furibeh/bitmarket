-- Seed stock for the SKUs of the seed catalog (V5). NOVA-X-256-AZUL is sold out on purpose
-- (CU-02 A2, RN-12).

INSERT INTO stock (sku, available) VALUES
    ('NOVA-X-128-NEGRO', 12),
    ('NOVA-X-256-NEGRO', 5),
    ('NOVA-X-256-AZUL', 0),
    ('ANDES-14-8-256', 8),
    ('ANDES-14-16-512', 3),
    ('BRISA-NEGRO', 25),
    ('BRISA-BLANCO', 1),
    ('BRISA-ROJO', 10),
    ('NOVA-MINI-64-NEGRO', 4);
