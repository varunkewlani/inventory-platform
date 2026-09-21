CREATE TABLE customers (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id  BIGINT       NOT NULL,
    name             VARCHAR(255) NOT NULL,
    email            VARCHAR(255),
    phone            VARCHAR(50),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_customers_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB;

CREATE INDEX idx_customers_organization_id ON customers (organization_id);

CREATE TABLE orders (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id   BIGINT        NOT NULL,
    warehouse_id      BIGINT        NOT NULL,
    customer_id       BIGINT        NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    total_amount      DECIMAL(14,2) NOT NULL,
    idempotency_key   VARCHAR(255),
    created_by        BIGINT        NOT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_orders_org_idempotency_key UNIQUE (organization_id, idempotency_key),
    CONSTRAINT fk_orders_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_orders_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_orders_organization_id ON orders (organization_id);
CREATE INDEX idx_orders_customer_id ON orders (customer_id);

CREATE TABLE order_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT        NOT NULL,
    product_id  BIGINT        NOT NULL,
    quantity    INT           NOT NULL,
    unit_price  DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
