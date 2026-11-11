-- Inventory module: available stock per SKU. The SKU references a catalog variant by identifier
-- only, without a foreign key. The CHECK is the safety net of RN-30.

CREATE TABLE stock (
    sku       text PRIMARY KEY,
    available integer NOT NULL CHECK (available >= 0)
);
