package com.fazzimart.dao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.fazzimart.model.CartItem;
import com.fazzimart.util.ExcelUtil;

/**
 * DAO for cart_items.xlsx.
 * Columns: cart_id | user_id | product_id | quantity
 */
@Repository
public class CartDao {

    public static final String[] HEADERS = {
            "cart_id", "user_id", "product_id", "quantity"
    };

    private static final int COL_ID = 0;
    private static final int COL_USER_ID = 1;
    private static final int COL_PRODUCT_ID = 2;
    private static final int COL_QUANTITY = 3;

    private File file() {
        return ExcelUtil.dataFile("cart_items.xlsx");
    }

    public List<CartItem> findByUserId(Long userId) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "cart_items", HEADERS);
                List<CartItem> items = new ArrayList<>();
                for (String[] row : rows) {
                    if (row[COL_USER_ID].trim().equals(String.valueOf(userId))) {
                        items.add(toItem(row));
                    }
                }
                return items;
            } catch (IOException e) {
                throw new RuntimeException("Could not read cart_items.xlsx", e);
            }
        }
    }

    public CartItem findByUserIdAndProductId(Long userId, Long productId) {
        for (CartItem item : findByUserId(userId)) {
            if (item.getProductId().equals(productId)) {
                return item;
            }
        }
        return null;
    }

    /** Adds a new cart row (or updates the quantity if the item is already there). */
    public void addOrUpdate(Long userId, Long productId, Integer quantity) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "cart_items", HEADERS);

                for (String[] row : rows) {
                    if (row[COL_USER_ID].trim().equals(String.valueOf(userId))
                            && row[COL_PRODUCT_ID].trim().equals(String.valueOf(productId))) {
                        row[COL_QUANTITY] = String.valueOf(quantity);
                        ExcelUtil.writeRows(file(), "cart_items", HEADERS, rows);
                        return;
                    }
                }

                long id = ExcelUtil.nextId(rows, COL_ID, 1);
                String[] row = new String[HEADERS.length];
                row[COL_ID] = String.valueOf(id);
                row[COL_USER_ID] = String.valueOf(userId);
                row[COL_PRODUCT_ID] = String.valueOf(productId);
                row[COL_QUANTITY] = String.valueOf(quantity);
                rows.add(row);
                ExcelUtil.writeRows(file(), "cart_items", HEADERS, rows);
            } catch (IOException e) {
                throw new RuntimeException("Could not write cart_items.xlsx", e);
            }
        }
    }

    public void setQuantity(Long userId, Long productId, Integer quantity) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "cart_items", HEADERS);
                for (String[] row : rows) {
                    if (row[COL_USER_ID].trim().equals(String.valueOf(userId))
                            && row[COL_PRODUCT_ID].trim().equals(String.valueOf(productId))) {
                        row[COL_QUANTITY] = String.valueOf(quantity);
                        break;
                    }
                }
                ExcelUtil.writeRows(file(), "cart_items", HEADERS, rows);
            } catch (IOException e) {
                throw new RuntimeException("Could not write cart_items.xlsx", e);
            }
        }
    }

    public void remove(Long userId, Long productId) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "cart_items", HEADERS);
                rows.removeIf(row -> row[COL_USER_ID].trim().equals(String.valueOf(userId))
                        && row[COL_PRODUCT_ID].trim().equals(String.valueOf(productId)));
                ExcelUtil.writeRows(file(), "cart_items", HEADERS, rows);
            } catch (IOException e) {
                throw new RuntimeException("Could not write cart_items.xlsx", e);
            }
        }
    }

    public void clear(Long userId) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "cart_items", HEADERS);
                rows.removeIf(row -> row[COL_USER_ID].trim().equals(String.valueOf(userId)));
                ExcelUtil.writeRows(file(), "cart_items", HEADERS, rows);
            } catch (IOException e) {
                throw new RuntimeException("Could not write cart_items.xlsx", e);
            }
        }
    }

    private CartItem toItem(String[] row) {
        return new CartItem(
                parseLong(row[COL_ID]),
                parseLong(row[COL_USER_ID]),
                parseLong(row[COL_PRODUCT_ID]),
                parseInt(row[COL_QUANTITY]));
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}