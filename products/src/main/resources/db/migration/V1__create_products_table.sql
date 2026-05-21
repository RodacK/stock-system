CREATE TABLE products (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(150)    NOT NULL UNIQUE,
    price       NUMERIC(12, 2)  NOT NULL,
    description TEXT
);