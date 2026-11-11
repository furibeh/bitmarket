-- Catalog module: categories, products and variants (RN-01, RN-02, RN-03, RN-05, RN-07).

CREATE TABLE category (
    id   uuid PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE product (
    id          uuid PRIMARY KEY,
    category_id uuid    NOT NULL REFERENCES category (id),
    name        text    NOT NULL,
    description text    NOT NULL,
    active      boolean NOT NULL
);

CREATE INDEX product_category_id_idx ON product (category_id);

CREATE TABLE variant (
    id         uuid PRIMARY KEY,
    product_id uuid    NOT NULL REFERENCES product (id),
    sku        text    NOT NULL UNIQUE,
    attributes jsonb   NOT NULL,
    price_cop  bigint  NOT NULL CHECK (price_cop > 0),
    active     boolean NOT NULL,
    -- Attributes are a flat key-value object with text values (RN-05).
    CONSTRAINT variant_attributes_key_value_check CHECK (
        jsonb_typeof(attributes) = 'object'
        AND NOT jsonb_path_exists(attributes, '$.* ? (@.type() != "string")')
    )
);

CREATE INDEX variant_product_id_idx ON variant (product_id);
