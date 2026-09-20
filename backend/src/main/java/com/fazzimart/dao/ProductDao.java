package com.fazzimart.dao;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.fazzimart.model.Product;
import com.fazzimart.util.ExcelUtil;

/**
 * DAO for products.xlsx.
 * Columns: id | name | description | category | price | image_url | stock | rating
 */
@Repository
public class ProductDao {

    public static final String[] HEADERS = {
            "id", "name", "description", "category", "price", "image_url", "stock", "rating"
    };

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_CATEGORY = 3;
    private static final int COL_PRICE = 4;
    private static final int COL_IMAGE = 5;
    private static final int COL_STOCK = 6;
    private static final int COL_RATING = 7;

    private File file() {
        return ExcelUtil.dataFile("products.xlsx");
    }

    public List<Product> findAll() {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "products", HEADERS);
                List<Product> products = new ArrayList<>();
                for (String[] row : rows) {
                    products.add(toProduct(row));
                }
                return products;
            } catch (IOException e) {
                throw new RuntimeException("Could not read products.xlsx", e);
            }
        }
    }

    public Product findById(Long id) {
        if (id == null) {
            return null;
        }
        for (Product p : findAll()) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public boolean existsByName(String name) {
        for (Product p : findAll()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public long count() {
        synchronized (ExcelUtil.LOCK) {
            try {
                return ExcelUtil.readRows(file(), "products", HEADERS).size();
            } catch (IOException e) {
                throw new RuntimeException("Could not read products.xlsx", e);
            }
        }
    }

    /** Appends a new product row and assigns the next id. */
    public Product save(Product product) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "products", HEADERS);
                long id = ExcelUtil.nextId(rows, COL_ID, 1);
                rows.add(toRow(product, id));
                ExcelUtil.writeRows(file(), "products", HEADERS, rows);
                product.setId(id);
                return product;
            } catch (IOException e) {
                throw new RuntimeException("Could not write products.xlsx", e);
            }
        }
    }

    /** Replaces the row with the same id. */
    public Product update(Product product) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "products", HEADERS);
                for (int i = 0; i < rows.size(); i++) {
                    if (rows.get(i)[COL_ID].trim().equals(String.valueOf(product.getId()))) {
                        rows.set(i, toRow(product, product.getId()));
                        break;
                    }
                }
                ExcelUtil.writeRows(file(), "products", HEADERS, rows);
                return product;
            } catch (IOException e) {
                throw new RuntimeException("Could not write products.xlsx", e);
            }
        }
    }

    public void delete(Long id) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "products", HEADERS);
                rows.removeIf(row -> row[COL_ID].trim().equals(String.valueOf(id)));
                ExcelUtil.writeRows(file(), "products", HEADERS, rows);
            } catch (IOException e) {
                throw new RuntimeException("Could not write products.xlsx", e);
            }
        }
    }

    /** Used by the seeder to add the initial catalogue. */
    public void seed(List<Product> products) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> rows = ExcelUtil.readRows(file(), "products", HEADERS);
                long id = ExcelUtil.nextId(rows, COL_ID, 1);
                for (Product p : products) {
                    rows.add(toRow(p, id++));
                }
                ExcelUtil.writeRows(file(), "products", HEADERS, rows);
            } catch (IOException e) {
                throw new RuntimeException("Could not write products.xlsx", e);
            }
        }
    }

    private Product toProduct(String[] row) {
        return new Product(
                parseLong(row[COL_ID]),
                row[COL_NAME],
                row[COL_DESCRIPTION],
                row[COL_CATEGORY],
                parseDecimal(row[COL_PRICE]),
                row[COL_IMAGE],
                parseInt(row[COL_STOCK]),
                parseDecimal(row[COL_RATING]));
    }

    private String[] toRow(Product p, long id) {
        String[] row = new String[HEADERS.length];
        row[COL_ID] = String.valueOf(id);
        row[COL_NAME] = p.getName();
        row[COL_DESCRIPTION] = p.getDescription();
        row[COL_CATEGORY] = p.getCategory();
        row[COL_PRICE] = p.getPrice() == null ? "0" : p.getPrice().toPlainString();
        row[COL_IMAGE] = p.getImageUrl();
        row[COL_STOCK] = String.valueOf(p.getStock() == null ? 0 : p.getStock());
        row[COL_RATING] = p.getRating() == null ? "4.5" : p.getRating().toPlainString();
        return row;
    }

    private BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
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