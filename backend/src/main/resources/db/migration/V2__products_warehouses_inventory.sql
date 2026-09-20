CREATE TABLE warehouses (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id  BIGINT       NOT NULL,
    name             VARCHAR(255) NOT NULL,
    address          VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_warehouses_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB;

CREATE INDEX idx_warehouses_organization_id ON warehouses (organization_id);

CREATE TABLE products (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id  BIGINT        NOT NULL,
    sku              VARCHAR(100)  NOT NULL,
    name             VARCHAR(255)  NOT NULL,
    description      VARCHAR(2000),
    price            DECIMAL(12,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_products_org_sku UNIQUE (organization_id, sku),
    CONSTRAINT fk_products_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB;

CREATE INDEX idx_products_organization_id ON products (organization_id);

-- available_quantity/reserved_quantity CHECK constraints are the DB-level
-- backstop for "inventory must never go negative" — the application never
-- relies on them (the atomic conditional UPDATEs in InventoryRepository are
-- the real mechanism), but they mean the invariant holds even against a
-- hypothetical future bug or a stray manual UPDATE.
CREATE TABLE inventory (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id     BIGINT    NOT NULL,
    warehouse_id        BIGINT    NOT NULL,
    product_id          BIGINT    NOT NULL,
    available_quantity  INT       NOT NULL DEFAULT 0,
    reserved_quantity    INT       NOT NULL DEFAULT 0,
    version             BIGINT    NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_inventory_warehouse_product UNIQUE (warehouse_id, product_id),
    CONSTRAINT fk_inventory_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_inventory_available_nonneg CHECK (available_quantity >= 0),
    CONSTRAINT chk_inventory_reserved_nonneg CHECK (reserved_quantity >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_inventory_organization_id ON inventory (organization_id);
CREATE INDEX idx_inventory_product_id ON inventory (product_id);

-- Append-only history of every inventory movement (add/remove/transfer/
-- reserve/release), satisfying spec §6's "inventory history" requirement.
CREATE TABLE inventory_movements (
    id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id            BIGINT      NOT NULL,
    inventory_id               BIGINT      NOT NULL,
    warehouse_id               BIGINT      NOT NULL,
    product_id                 BIGINT      NOT NULL,
    movement_type              VARCHAR(20) NOT NULL,
    quantity                   INT         NOT NULL,
    available_quantity_after   INT         NOT NULL,
    reserved_quantity_after    INT         NOT NULL,
    note                       VARCHAR(500),
    created_by                 BIGINT,
    created_at                 TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_mvmt_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_inv_mvmt_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (id),
    CONSTRAINT fk_inv_mvmt_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_inv_mvmt_organization_id ON inventory_movements (organization_id);
CREATE INDEX idx_inv_mvmt_inventory_id ON inventory_movements (inventory_id);
