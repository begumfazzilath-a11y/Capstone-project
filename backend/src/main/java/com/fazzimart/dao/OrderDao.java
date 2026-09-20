package com.fazzimart.dao;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.fazzimart.model.Order;
import com.fazzimart.model.OrderItem;
import com.fazzimart.util.ExcelUtil;

/**
 * DAO for orders.xlsx + order_items.xlsx.
 *
 * orders.xlsx columns:
 *   order_id | user_id | total_amount | order_date | status
 *   | customer_name | phone | address | city | postal_code
 *
 * order_items.xlsx columns:
 *   order_item_id | order_id | product_id | product_name | quantity | price
 */
@Repository
public class OrderDao {

    public static final String[] ORDER_HEADERS = {
            "order_id", "user_id", "total_amount", "order_date", "status",
            "customer_name", "phone", "address", "city", "postal_code"
    };

    public static final String[] ITEM_HEADERS = {
            "order_item_id", "order_id", "product_id", "product_name", "quantity", "price"
    };

    private static final int OC_ID = 0;
    private static final int OC_USER_ID = 1;
    private static final int OC_TOTAL = 2;
    private static final int OC_DATE = 3;
    private static final int OC_STATUS = 4;
    private static final int OC_NAME = 5;
    private static final int OC_PHONE = 6;
    private static final int OC_ADDRESS = 7;
    private static final int OC_CITY = 8;
    private static final int OC_POSTAL = 9;

    private static final int IC_ID = 0;
    private static final int IC_ORDER_ID = 1;
    private static final int IC_PRODUCT_ID = 2;
    private static final int IC_PRODUCT_NAME = 3;
    private static final int IC_QUANTITY = 4;
    private static final int IC_PRICE = 5;

    private File ordersFile() {
        return ExcelUtil.dataFile("orders.xlsx");
    }

    private File itemsFile() {
        return ExcelUtil.dataFile("order_items.xlsx");
    }

    /** Appends the order and all its items. Caller must hold ExcelUtil.LOCK. */
    public Order save(Order order) {
        try {
            List<String[]> orders = ExcelUtil.readRows(ordersFile(), "orders", ORDER_HEADERS);
            long orderId = ExcelUtil.nextId(orders, OC_ID, 1);

            String[] oRow = new String[ORDER_HEADERS.length];
            oRow[OC_ID] = String.valueOf(orderId);
            oRow[OC_USER_ID] = String.valueOf(order.getUserId());
            oRow[OC_TOTAL] = order.getTotalAmount() == null ? "0" : order.getTotalAmount().toPlainString();
            oRow[OC_DATE] = ExcelUtil.format(order.getOrderDate() == null
                    ? java.time.LocalDateTime.now() : order.getOrderDate());
            oRow[OC_STATUS] = order.getStatus();
            oRow[OC_NAME] = order.getCustomerName();
            oRow[OC_PHONE] = order.getPhone();
            oRow[OC_ADDRESS] = order.getAddress();
            oRow[OC_CITY] = order.getCity();
            oRow[OC_POSTAL] = order.getPostalCode();
            orders.add(oRow);
            ExcelUtil.writeRows(ordersFile(), "orders", ORDER_HEADERS, orders);

            List<String[]> items = ExcelUtil.readRows(itemsFile(), "order_items", ITEM_HEADERS);
            long itemId = ExcelUtil.nextId(items, IC_ID, 1);
            for (OrderItem item : order.getItems()) {
                String[] iRow = new String[ITEM_HEADERS.length];
                iRow[IC_ID] = String.valueOf(itemId++);
                iRow[IC_ORDER_ID] = String.valueOf(orderId);
                iRow[IC_PRODUCT_ID] = String.valueOf(item.getProductId());
                iRow[IC_PRODUCT_NAME] = item.getProductName();
                iRow[IC_QUANTITY] = String.valueOf(item.getQuantity());
                iRow[IC_PRICE] = item.getPrice() == null ? "0" : item.getPrice().toPlainString();
                items.add(iRow);
            }
            ExcelUtil.writeRows(itemsFile(), "order_items", ITEM_HEADERS, items);

            order.setId(orderId);
            order.getItems().forEach(i -> i.setOrderId(orderId));
            return order;
        } catch (IOException e) {
            throw new RuntimeException("Could not write orders.xlsx", e);
        }
    }

    public List<Order> findByUserId(Long userId) {
        synchronized (ExcelUtil.LOCK) {
            try {
                List<String[]> orders = ExcelUtil.readRows(ordersFile(), "orders", ORDER_HEADERS);
                List<Order> result = new ArrayList<>();
                for (String[] row : orders) {
                    if (row[OC_USER_ID].trim().equals(String.valueOf(userId))) {
                        result.add(toOrder(row, itemsFor(Long.parseLong(row[OC_ID].trim()))));
                    }
                }
                return result;
            } catch (IOException e) {
                throw new RuntimeException("Could not read orders.xlsx", e);
            }
        }
    }

    public Order findByIdAndUserId(Long orderId, Long userId) {
        synchronized (ExcelUtil.LOCK) {
            try {
                for (String[] row : ExcelUtil.readRows(ordersFile(), "orders", ORDER_HEADERS)) {
                    if (row[OC_ID].trim().equals(String.valueOf(orderId))
                            && row[OC_USER_ID].trim().equals(String.valueOf(userId))) {
                        return toOrder(row, itemsFor(orderId));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read orders.xlsx", e);
            }
        }
        return null;
    }

    private List<OrderItem> itemsFor(Long orderId) throws IOException {
        List<String[]> items = ExcelUtil.readRows(itemsFile(), "order_items", ITEM_HEADERS);
        List<OrderItem> result = new ArrayList<>();
        for (String[] row : items) {
            if (row[IC_ORDER_ID].trim().equals(String.valueOf(orderId))) {
                result.add(new OrderItem(
                        parseLong(row[IC_ID]),
                        orderId,
                        parseLong(row[IC_PRODUCT_ID]),
                        row[IC_PRODUCT_NAME],
                        parseInt(row[IC_QUANTITY]),
                        parseDecimal(row[IC_PRICE])));
            }
        }
        return result;
    }

    private Order toOrder(String[] row, List<OrderItem> items) {
        Order order = new Order();
        order.setId(parseLong(row[OC_ID]));
        order.setUserId(parseLong(row[OC_USER_ID]));
        order.setTotalAmount(parseDecimal(row[OC_TOTAL]));
        order.setOrderDate(ExcelUtil.parse(row[OC_DATE]));
        order.setStatus(row[OC_STATUS]);
        order.setCustomerName(row[OC_NAME]);
        order.setPhone(row[OC_PHONE]);
        order.setAddress(row[OC_ADDRESS]);
        order.setCity(row[OC_CITY]);
        order.setPostalCode(row[OC_POSTAL]);
        order.setItems(items);
        return order;
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