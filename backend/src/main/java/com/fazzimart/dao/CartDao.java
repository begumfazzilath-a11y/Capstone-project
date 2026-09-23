package com.fazzimart.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.fazzimart.model.CartItem;
import com.fazzimart.util.DBConnection;

/**
 * DAO for the MySQL {@code cart_items} table (same pattern as DhanyaMart's DAOs).
 * Columns: id | user_id | product_id | quantity.
 *
 * The unique key on (user_id, product_id) lets {@code addOrUpdate} use an
 * {@code INSERT ... ON DUPLICATE KEY UPDATE} so a re-add simply raises the
 * quantity instead of creating a second row.
 */
@Repository
public class CartDao {

    private static final String COLUMNS =
            "id, user_id, product_id, quantity";

    /** Returns every cart row for one user (in insertion order). */
    public List<CartItem> findByUserId(Long userId) {
        List<CartItem> items = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM cart_items WHERE user_id = ? ORDER BY id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(toItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read cart items from MySQL", e);
        }
        return items;
    }

    /** Returns the matching cart row, or null when the product is not in the cart. */
    public CartItem findByUserIdAndProductId(Long userId, Long productId) {
        String sql = "SELECT " + COLUMNS + " FROM cart_items WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toItem(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read cart item from MySQL", e);
        }
        return null;
    }

    /**
     * Adds a new cart row, or updates the quantity if the item is already there
     * (the unique key on user_id + product_id makes this atomic).
     */
    public void addOrUpdate(Long userId, Long productId, Integer quantity) {
        String sql = "INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not add cart item to MySQL", e);
        }
    }

    public void setQuantity(Long userId, Long productId, Integer quantity) {
        String sql = "UPDATE cart_items SET quantity = ? WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setLong(2, userId);
            ps.setLong(3, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update cart item in MySQL", e);
        }
    }

    public void remove(Long userId, Long productId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not remove cart item in MySQL", e);
        }
    }

    public void clear(Long userId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not clear cart in MySQL", e);
        }
    }

    private CartItem toItem(ResultSet rs) throws SQLException {
        return new CartItem(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("product_id"),
                rs.getInt("quantity"));
    }
}