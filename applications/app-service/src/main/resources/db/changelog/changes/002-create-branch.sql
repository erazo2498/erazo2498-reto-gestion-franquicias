--liquibase formatted sql

--changeset brayan:002-create-branch
CREATE TABLE branch (
    id           UUID         PRIMARY KEY,
    franchise_id UUID         NOT NULL,
    name         VARCHAR(120) NOT NULL,
    CONSTRAINT fk_branch_franchise FOREIGN KEY (franchise_id) REFERENCES franchise (id),
    CONSTRAINT ck_branch_name_length CHECK (char_length(name) BETWEEN 1 AND 120)
);

-- Also serves lookups by franchise_id, since it is the first column of the index.
CREATE UNIQUE INDEX uq_branch_franchise_name_lower ON branch (franchise_id, lower(name));

--rollback DROP TABLE branch;
