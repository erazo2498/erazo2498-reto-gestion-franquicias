--liquibase formatted sql

--changeset brayan:003-create-product
CREATE TABLE product (
    id        UUID         PRIMARY KEY,
    branch_id UUID         NOT NULL,
    name      VARCHAR(120) NOT NULL,
    stock     INTEGER      NOT NULL,
    CONSTRAINT fk_product_branch FOREIGN KEY (branch_id) REFERENCES branch (id),
    CONSTRAINT ck_product_name_length CHECK (char_length(name) BETWEEN 1 AND 120),
    CONSTRAINT ck_product_stock_not_negative CHECK (stock >= 0)
);

-- Also serves lookups by branch_id, since it is the first column of the index.
CREATE UNIQUE INDEX uq_product_branch_name_lower ON product (branch_id, lower(name));

--rollback DROP TABLE product;
