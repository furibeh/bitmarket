-- Seed catalog of the walking skeleton. The stock of these SKUs is seeded by the inventory module
-- in V6.

INSERT INTO category (id, name) VALUES
    ('0a000000-0000-4000-8000-000000000001', 'Celulares'),
    ('0a000000-0000-4000-8000-000000000002', 'Portátiles'),
    ('0a000000-0000-4000-8000-000000000003', 'Audio');

INSERT INTO product (id, category_id, name, description, active) VALUES
    ('0b000000-0000-4000-8000-000000000001', '0a000000-0000-4000-8000-000000000001',
     'Celular Nova X', 'Pantalla OLED de 6,5 pulgadas y cámara de 50 MP.', true),
    ('0b000000-0000-4000-8000-000000000002', '0a000000-0000-4000-8000-000000000002',
     'Portátil Andes 14', 'Portátil ultraliviano de 14 pulgadas para trabajo y estudio.', true),
    ('0b000000-0000-4000-8000-000000000003', '0a000000-0000-4000-8000-000000000003',
     'Audífonos Brisa', 'Audífonos inalámbricos con cancelación de ruido.', true),
    ('0b000000-0000-4000-8000-000000000004', '0a000000-0000-4000-8000-000000000001',
     'Celular Nova Mini', 'Modelo descontinuado.', false);

INSERT INTO variant (id, product_id, sku, attributes, price_cop, active) VALUES
    ('0c000000-0000-4000-8000-000000000001', '0b000000-0000-4000-8000-000000000001',
     'NOVA-X-128-NEGRO', '{"capacidad": "128GB", "color": "negro"}', 2499000, true),
    ('0c000000-0000-4000-8000-000000000002', '0b000000-0000-4000-8000-000000000001',
     'NOVA-X-256-NEGRO', '{"capacidad": "256GB", "color": "negro"}', 2899000, true),
    ('0c000000-0000-4000-8000-000000000003', '0b000000-0000-4000-8000-000000000001',
     'NOVA-X-256-AZUL', '{"capacidad": "256GB", "color": "azul"}', 2899000, true),
    ('0c000000-0000-4000-8000-000000000004', '0b000000-0000-4000-8000-000000000002',
     'ANDES-14-8-256', '{"memoria": "8GB", "almacenamiento": "256GB"}', 3299000, true),
    ('0c000000-0000-4000-8000-000000000005', '0b000000-0000-4000-8000-000000000002',
     'ANDES-14-16-512', '{"memoria": "16GB", "almacenamiento": "512GB"}', 4199000, true),
    ('0c000000-0000-4000-8000-000000000006', '0b000000-0000-4000-8000-000000000003',
     'BRISA-NEGRO', '{"color": "negro"}', 459000, true),
    ('0c000000-0000-4000-8000-000000000007', '0b000000-0000-4000-8000-000000000003',
     'BRISA-BLANCO', '{"color": "blanco"}', 459000, true),
    ('0c000000-0000-4000-8000-000000000008', '0b000000-0000-4000-8000-000000000003',
     'BRISA-ROJO', '{"color": "rojo"}', 479000, false),
    ('0c000000-0000-4000-8000-000000000009', '0b000000-0000-4000-8000-000000000004',
     'NOVA-MINI-64-NEGRO', '{"capacidad": "64GB", "color": "negro"}', 1299000, true);
