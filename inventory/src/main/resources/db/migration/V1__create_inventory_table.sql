CREATE TABLE inventory (
    product_id  BIGINT   PRIMARY KEY,
    quantity    INTEGER  NOT NULL CHECK (quantity >= 0)
);