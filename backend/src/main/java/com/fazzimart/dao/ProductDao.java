package com.fazzimart.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.fazzimart.model.Product;
import com.fazzimart.util.DBConnection;

/**
 * DAO for the MySQL {@code products} table (same pattern as DhanyaMart's ProductDAO).
 * All SQL uses prepared statements and MySQL's {@code products.id}
 * auto-increment column generates the ids.
 *
 * Columns: id | name | description | category | price | image_url | stock | rating
 */
@Repository
public class ProductDao {

    private static final String COLUMNS =
            "id, name, description, category, price, image_url, stock, rating";

    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM products ORDER BY id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(toProduct(rs));
            }
            return products;
        } catch (SQLException e) {
            throw new RuntimeException("Could not read products from MySQL", e);
        }
    }

    public Product findById(Long id) {
        if (id == null) {
            return null;
        }
        String sql = "SELECT " + COLUMNS + " FROM products WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toProduct(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read product by id from MySQL", e);
        }
        return null;
    }

    /** Returns true when a product with the same name (case-insensitive) exists. */
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM products WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not check product name in MySQL", e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not count products in MySQL", e);
        }
    }

    /** Inserts a new product row and assigns the generated id. */
    public Product save(Product product) {
        String sql = "INSERT INTO products (name, description, category, price, image_url, stock, rating) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setString(3, product.getCategory());
            ps.setBigDecimal(4, product.getPrice());
            ps.setString(5, product.getImageUrl());
            ps.setInt(6, product.getStock() == null ? 0 : product.getStock());
            ps.setBigDecimal(7, product.getRating() == null ? new BigDecimal("4.5") : product.getRating());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    product.setId(keys.getLong(1));
                }
            }
            return product;
        } catch (SQLException e) {
            throw new RuntimeException("Could not insert product into MySQL", e);
        }
    }

    /** Updates the product row with the same id. */
    public Product update(Product product) {
        String sql = "UPDATE products SET name = ?, description = ?, category = ?, price = ?, "
                + "image_url = ?, stock = ?, rating = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setString(3, product.getCategory());
            ps.setBigDecimal(4, product.getPrice());
            ps.setString(5, product.getImageUrl());
            ps.setInt(6, product.getStock() == null ? 0 : product.getStock());
            ps.setBigDecimal(7, product.getRating() == null ? new BigDecimal("4.5") : product.getRating());
            ps.setLong(8, product.getId());
            ps.executeUpdate();
            return product;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update product in MySQL", e);
        }
    }

    /**
     * Deletes a product row. Rows in {@code cart_items} are removed by the
     * FK CASCADE, past {@code order_items} keep their snapshot (product_id set
     * to NULL by the FK action).
     */
    public void delete(Long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete product in MySQL", e);
        }
    }

    /** Used by the seeder to add the initial catalogue. */
    public void seed(List<Product> products) {
        for (Product product : products) {
            save(product);
        }
    }

    private Product toProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getBigDecimal("price"),
                rs.getString("image_url"),
                rs.getInt("stock"),
                rs.getBigDecimal("rating"));
    }
}