-- =====================================================================
--  FAZZI MART - MySQL 8 schema
-- =====================================================================
--  This script creates the `fazzimart` database and every table the
--  application uses (users, products, cart_items, orders, order_items).
--  The DAO layer also auto-runs the SAME CREATE TABLE IF NOT EXISTS
--  statements on first connect, so running this file by hand is optional
--  - but it is the authoritative schema for the review.
--
--  Run it inside the MySQL client:
--      mysql -u root -p < db/fazzi_mart.sql
--
--  Tables:
--    users        authentication + profile info (role: USER / ADMIN)
--    products     product catalogue (image_url stored; served from frontend/)
--    cart_items   one row per product per user (unique on user+product)
--    orders       an order snapshot placed at checkout
--    order_items  line items of an order (FK -> orders, product_id snapshot)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS fazzimart
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE fazzimart;

-- ----------------------------------------------------------------
-- users
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL COMMENT 'BCrypt hash, never plain text',
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- products
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)  NOT NULL,
    description TEXT          NOT NULL,
    category    VARCHAR(50)   NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    image_url   VARCHAR(255)  NOT NULL,
    stock       INT           NOT NULL DEFAULT 0,
    rating      DECIMAL(2,1)  NOT NULL DEFAULT 4.5,
    PRIMARY KEY (id),
    KEY idx_products_category (category)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- cart_items
-- Deleting a product (admin) removes its rows here via CASCADE.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_product (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- orders
-- Deleting a user removes his orders via CASCADE.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    total_amount  DECIMAL(10,2) NOT NULL,
    order_date    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        VARCHAR(30)   NOT NULL DEFAULT 'PLACED',
    customer_name VARCHAR(100)  NOT NULL,
    phone         VARCHAR(20)   NOT NULL,
    address       VARCHAR(255)  NOT NULL,
    city          VARCHAR(100)  NOT NULL,
    postal_code   VARCHAR(20)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_orders_user (user_id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- order_items  (snapshot of each cart line at purchase time)
-- product_id is SET NULL when a product is later deleted so that past
-- orders keep their name/price snapshot intact.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NULL,
    product_name VARCHAR(150)  NOT NULL,
    quantity     INT           NOT NULL DEFAULT 1,
    price        DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    KEY idx_order_items_product (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- Admin + sample products are seeded automatically by the backend on
-- first launch (config/DataSeeder.java), so the BCrypt hash is always
-- generated correctly. See README.md for the demo login.
-- ----------------------------------------------------------------