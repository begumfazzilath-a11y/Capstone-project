-- ============================================================
-- FAZZI MART - MySQL 8 Database Setup Script
-- Run this file with:  mysql -u root -p < database/fazzi_mart.sql
-- (Optional: Hibernate can auto-create the tables. This script
--  creates the schema explicitly plus the sample products.)
-- ============================================================

CREATE DATABASE IF NOT EXISTS fazzi_mart
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE fazzi_mart;

-- ------------------------------------------------------------
-- users table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    password   VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- products table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)  NOT NULL,
    description TEXT          NOT NULL,
    category    VARCHAR(50)   NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    image_url   VARCHAR(255)  NOT NULL,
    stock       INT           NOT NULL,
    rating      DECIMAL(2,1)  NOT NULL DEFAULT 4.5,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- orders table
-- ------------------------------------------------------------
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
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- order_items table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    product_name VARCHAR(150)  NOT NULL,
    quantity     INT           NOT NULL,
    price        DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- cart_items table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_cart_user (user_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- Admin user
-- NOTE: The admin account (admin@fazzimart.com / Admin@123) is
-- auto-seeded by the backend on first launch (DataSeeder.java),
-- so its BCrypt password hash is always generated correctly.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- Sample products  (image paths are relative to frontend/)
-- ------------------------------------------------------------
INSERT INTO products (name, description, category, price, image_url, stock, rating) VALUES
('Premium Dog Food (Chicken & Rice)', 'Nutritious dry dog food packed with real chicken, brown rice, vitamins and omega fatty acids for a shiny coat and strong immunity.', 'Pet Food', 1299.00, 'images/dog-food.svg', 35, 4.8),
('Deluxe Cat Food (Salmon Recipe)', 'Premium salmon-based cat food rich in taurine, proteins and DHA to support healthy eyes, heart and digestion.', 'Pet Food', 1199.00, 'images/cat-food.svg', 28, 4.7),
('Puppy Starter Food', 'Specially formulated growth food for puppies up to 12 months with calcium, DHA and antioxidants for bone and brain development.', 'Pet Food', 1450.00, 'images/puppy-food.svg', 22, 4.6),
('Adult Dog Food (Beef Formula)', 'High-protein beef formula for adult dogs that provides lasting energy, lean muscle support and healthy skin.', 'Pet Food', 1399.00, 'images/adult-dog-food.svg', 30, 4.5),
('Pet Shampoo (Anti-Dandruff)', 'Gentle, hypoallergenic shampoo with oatmeal and aloe vera that cleans, deodorizes and soothes itchy skin.', 'Pet Products', 249.00, 'images/pet-shampoo.svg', 40, 4.4),
('Grooming Brush Set', 'Professional 3-piece grooming set with deshedding brush, slicker brush and comb for a tangle-free shiny coat.', 'Pet Products', 299.00, 'images/grooming-brush.svg', 25, 4.6),
('Pet Dental Kit (Toothbrush & Toothpaste)', 'Double-sided toothbrush and chicken-flavored toothpaste that fights plaque, tartar and bad breath.', 'Pet Products', 349.00, 'images/pet-toothbrush.svg', 18, 4.3),
('Pet Nail Clipper', 'Safezone nail clipper with a quick-stop guard to prevent over-cutting — perfect for dogs and cats.', 'Pet Products', 199.00, 'images/nail-clipper.svg', 0, 4.2),
('Adjustable Dog Collar', 'Durable, comfortable nylon collar with quick-release buckle and reflective stitching for night walks.', 'Pet Accessories', 259.00, 'images/dog-collar.svg', 32, 4.7),
('Stainless Steel Pet Bowl', 'Non-toxic, rust-free steel bowl with anti-slip rubber base and non-spill rim — available in two sizes.', 'Pet Accessories', 349.00, 'images/pet-bowl.svg', 45, 4.6),
('Dog Leash (Heavy Duty)', 'Strong 1.2m nylon leash with padded handle and heavy-duty metal clasp for full control on every walk.', 'Pet Accessories', 249.00, 'images/dog-leash.svg', 38, 4.5),
('Deluxe Pet Bed', 'Ultra-soft plush bed with removable, machine-washable cover and non-slip bottom for cozy naps.', 'Pet Accessories', 899.00, 'images/pet-bed.svg', 12, 4.8),
('Rubber Ball (Sturdy)', 'Durable, bite-resistant rubber ball that bounces high and floats in water — great for fetch at the park.', 'Pet Toys', 149.00, 'images/rubber-ball.svg', 50, 4.5),
('Rope Tug Toy', 'Braided cotton rope toy that cleans teeth and massages gums while your dog plays tug-of-war.', 'Pet Toys', 179.00, 'images/rope-toy.svg', 42, 4.4),
('Squeaky Duck Toy', 'Soft plush duck with a built-in squeaker that keeps your pet entertained for hours.', 'Pet Toys', 159.00, 'images/squeaky-toy.svg', 36, 4.3),
('Plush Catnip Mouse', 'Irresistible plush mouse stuffed with 100% organic catnip that drives your cat wild with fun.', 'Pet Toys', 129.00, 'images/plush-mouse.svg', 26, 4.6);