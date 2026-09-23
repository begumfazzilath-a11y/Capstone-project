package com.fazzimart.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.fazzimart.model.User;
import com.fazzimart.util.DBConnection;

/**
 * DAO for the MySQL {@code users} table (same pattern as DhanyaMart's UserDAO).
 * Every operation uses prepared statements and the {@code users.id}
 * auto-increment column generates the ids.
 *
 * Columns: id | name | phone | email | password | role | created_at
 */
@Repository
public class UserDao {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String COLUMNS =
            "id, name, phone, email, password, role, created_at";

    /** Returns every user row (used by the seeder and account pages). */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(toUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Could not read users from MySQL", e);
        }
    }

    /** Finds a user by email (email match is case-insensitive). */
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String sql = "SELECT " + COLUMNS + " FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not read user by email from MySQL", e);
        }
        return null;
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    /**
     * Inserts a new user row and assigns the generated id.
     * The password must already be hashed (the AuthService does this).
     */
    public User save(User user) {
        String sql = "INSERT INTO users (name, phone, email, password, role, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getRole() == null ? "USER" : user.getRole());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Could not insert user into MySQL", e);
        }
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not count users in MySQL", e);
        }
    }

    private User toUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role"),
                format(rs.getTimestamp("created_at")));
    }

    private String format(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        return ts.toLocalDateTime().format(TIMESTAMP);
    }
}