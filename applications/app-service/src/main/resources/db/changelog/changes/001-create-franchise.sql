--liquibase formatted sql

--changeset brayan:001-create-franchise
CREATE TABLE franchise (
    id   UUID         PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT ck_franchise_name_length CHECK (char_length(name) BETWEEN 1 AND 120)
);

CREATE UNIQUE INDEX uq_franchise_name_lower ON franchise (lower(name));

--rollback DROP TABLE franchise;
