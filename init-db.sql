-- Practica 5 - Saga y Circuit Breaker
-- Base de datos compartida, cada servicio usa sus propias tablas

-- ===================== ORDER SERVICE =====================
CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL PRIMARY KEY,
    product_id      VARCHAR(20)  NOT NULL,
    quantity        INTEGER      NOT NULL,
    amount          DOUBLE PRECISION NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    payment_id      BIGINT,
    inventory_id    BIGINT,
    shipping_id     BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ===================== PAYMENT SERVICE =====================
CREATE TABLE IF NOT EXISTS payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT       NOT NULL,
    amount          DOUBLE PRECISION NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ===================== INVENTORY SERVICE =====================
CREATE TABLE IF NOT EXISTS inventory_products (
    id              BIGSERIAL PRIMARY KEY,
    product_code    VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    stock           INTEGER      NOT NULL,
    fail_mode       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS inventory_reservations (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT       NOT NULL,
    product_code    VARCHAR(20)  NOT NULL,
    quantity        INTEGER      NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ===================== SHIPPING SERVICE =====================
CREATE TABLE IF NOT EXISTS shipments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT       NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    address         VARCHAR(200),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ===================== DATOS DE PRUEBA =====================
-- P001 tiene stock para el flujo feliz
-- P002 tiene stock 0 para simular fallo de inventario
INSERT INTO inventory_products (product_code, name, stock, fail_mode) VALUES
    ('P001', 'Teclado mecanico', 10, FALSE),
    ('P002', 'Mouse gamer', 0, FALSE);
