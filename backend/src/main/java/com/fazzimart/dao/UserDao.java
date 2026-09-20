package com.fazzimart.dao;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.fazzimart.model.User;
import com.fazzimart.util.ExcelUtil;

/**
 * DAO for users.xlsx (same pattern as DhanyaMart's UserDAO).
 * Columns: user_id | name | phone | email | password | role | created_at
 */
@Repository
public class UserDao {

    public static final String[] HEADERS = {
            "user_id", "name", "phone", "email", "password", "role", "created_at"
    };

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_PHONE = 2;
    private static final int COL_EMAIL = 3;
    private static final int COL_PASSWORD = 4;
    private static final int COL_ROLE = 5;
    private static final int COL_CREATED_AT = 6;

    private File file() {
        return ExcelUtil.dataFile("users.xlsx");
    }

    /** Returns every user row (used by the seeder and account pages). */
    public List<User> findAll() {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "users", HEADERS);
                List<User> users = new ArrayList<>();
                for (String[] row : rows) {
                    users.add(toUser(row));
                }
                return users;
            } catch (IOException e) {
                throw new RuntimeException("Could not read users.xlsx", e);
            }
        }
    }

    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        synchronized (ExcelUtil.LOCK) {
            try {
                for (String[] row : ExcelUtil.readRows(file(), "users", HEADERS)) {
                    if (email.equalsIgnoreCase(row[COL_EMAIL].trim())) {
                        return toUser(row);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read users.xlsx", e);
            }
        }
        return null;
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    /** Appends a new user row. The password must already be hashed. */
    public User save(User user) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "users", HEADERS);
                long id = ExcelUtil.nextId(rows, COL_ID, 1001);
                String[] row = new String[HEADERS.length];
                row[COL_ID] = String.valueOf(id);
                row[COL_NAME] = user.getName();
                row[COL_PHONE] = user.getPhone();
                row[COL_EMAIL] = user.getEmail();
                row[COL_PASSWORD] = user.getPassword();
                row[COL_ROLE] = user.getRole() == null ? "USER" : user.getRole();
                row[COL_CREATED_AT] = ExcelUtil.format(LocalDateTime.now());
                rows.add(row);
                ExcelUtil.writeRows(file(), "users", HEADERS, rows);
                user.setId(id);
                return user;
            } catch (IOException e) {
                throw new RuntimeException("Could not write users.xlsx", e);
            }
        }
    }

    public long count() {
        synchronized (ExcelUtil.LOCK) {
            try {
                return ExcelUtil.readRows(file(), "users", HEADERS).size();
            } catch (IOException e) {
                throw new RuntimeException("Could not read users.xlsx", e);
            }
        }
    }

    private User toUser(String[] row) {
        return new User(
                parseLong(row[COL_ID]),
                row[COL_NAME],
                row[COL_PHONE],
                row[COL_EMAIL],
                row[COL_PASSWORD],
                row[COL_ROLE],
                row[COL_CREATED_AT]);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0L;
        }
    }
}