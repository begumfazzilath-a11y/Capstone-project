package com.fazzimart.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.fazzimart.model.Order;
import com.fazzimart.model.OrderItem;
import com.fazzimart.util.DBConnection;

/**
 * DAO for the MySQL {@code orders} and {@code order_items} tables
 * (same pattern as DhanyaMart's DAOs).
 *
 * orders columns:   id | user_id | total_amount | order_date | status
 *                   | customer_name | phone | address | city | postal_code
 *
 * order_items columns: id | order_id | product_id | product_name | quantity | price
 *
 * {@code save()} is designed to run inside an explicit DB transaction
 * (see {@link com.fazzimart.service.OrderService}) and re-uses the thread-
 * local connection when one is open.
 */
@Repository
public class OrderDao {

    private static final String ORDER_COLUMNS =
            "id, user_id, total_amount, order_date, status, "
                    + "customer_name, phone, address, city, postal_code";

    private static final String ITEM_COLUMNS =
            "id, order_id, product_id, product_name, quantity, price";

    /**
     * Inserts the order and all its items. The generated order id is set on
     * the Order object and propagated to each OrderItem. When called from a
     * transaction the thread-local connection is reused so both inserts are
     * atomic.
     */
    public Order save(Order order) {
        String orderSql = "INSERT INTO orders (user_id, total_amount, order_date, status, "
                + "customer_name, phone, address, city, postal_code) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, order.getUserId());
            ps.setBigDecimal(2, order.getTotalAmount());
            ps.setTimestamp(3, Timestamp.valueOf(
                    order.getOrderDate() == null ? LocalDateTime.now() : order.getOrderDate()));
            ps.setString(4, order.getStatus());
            ps.setString(5, order.getCustomerName());
            ps.setString(6, order.getPhone());
            ps.setString(7, order.getAddress());
            ps.setString(8, order.getCity());
            ps.setString(9, order.getPostalCode());
            ps.executeUpdate();

            long orderId;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to get generated order id");
                }
                orderId = keys.getLong(1);
            }
            order.setId(orderId);

            // insert order items
            String itemSql = "INSERT INTO order_items (order_id, product_id, product_name, quantity, price) "
                    + "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ips = conn.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS)) {
                for (OrderItem item : order.getItems()) {
                    item.setOrderId(orderId);
                    ips.setLong(1, orderId);
                    if (item.getProductId() != null) {
                        ips.setLong(2, item.getProductId());
                    } else {
                        ips.setNull(2, Types.BIGINT);
                    }
                    ips.setString(3, item.getProductName());
                    ips.setInt(4, item.getQuantity());
                    ips.setBigDecimal(5, item.getPrice());
                    ips.addBatch();
                }
                ips.executeBatch();

                // set generated item ids on each item
                try (ResultSet keys = ips.getGeneratedKeys()) {
                    int idx = 0;
                    while (keys.next() && idx < order.getItems().size()) {
                        order.getItems().get(idx).setId(keys.getLong(1));
                        idx++;
                    }
                }
            }
            return order;
        } catch (SQLException e) {
            throw new RuntimeException("Could not insert order into MySQL", e);
        }
    }

    /** Returns all orders for one user (newest first). */
    public List<Order> findByUserId(Long userId) {
        List<Order> result = new ArrayList<>();
        String sql = "SELECT " + ORDER_COLUMNS + " FROM orders WHERE user_id = ? ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long orderId = rs.getLong("id");
                    List<OrderItem> items = itemsFor(conn, orderId);
                    result.add(toOrder(rs, items));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read orders from MySQL", e);
        }
        return result;
    }

    /** Returns one order if it belongs to the given user, else null. */
    public Order findByIdAndUserId(Long orderId, Long userId) {
        String sql = "SELECT " + ORDER_COLUMNS + " FROM orders WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toOrder(rs, itemsFor(conn, orderId));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read order from MySQL", e);
        }
        return null;
    }

    /** Loads all items belonging to one order id. */
    private List<OrderItem> itemsFor(Connection conn, long orderId) throws SQLException {
        List<OrderItem> result = new ArrayList<>();
        String sql = "SELECT " + ITEM_COLUMNS + " FROM order_items WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new OrderItem(
                            rs.getLong("id"),
                            orderId,
                            rs.getObject("product_id", Long.class),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("price")));
                }
            }
        }
        return result;
    }

    private Order toOrder(ResultSet rs, List<OrderItem> items) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setUserId(rs.getLong("user_id"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        Timestamp orderDate = rs.getTimestamp("order_date");
        order.setOrderDate(orderDate == null ? null : orderDate.toLocalDateTime());
        order.setStatus(rs.getString("status"));
        order.setCustomerName(rs.getString("customer_name"));
        order.setPhone(rs.getString("phone"));
        order.setAddress(rs.getString("address"));
        order.setCity(rs.getString("city"));
        order.setPostalCode(rs.getString("postal_code"));
        order.setItems(items);
        return order;
    }
}