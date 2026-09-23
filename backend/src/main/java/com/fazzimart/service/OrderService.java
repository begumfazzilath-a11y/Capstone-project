package com.fazzimart.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fazzimart.dao.CartDao;
import com.fazzimart.dao.OrderDao;
import com.fazzimart.dao.ProductDao;
import com.fazzimart.dto.CheckoutRequest;
import com.fazzimart.dto.OrderDTO;
import com.fazzimart.exception.ApiException;
import com.fazzimart.model.CartItem;
import com.fazzimart.model.Order;
import com.fazzimart.model.OrderItem;
import com.fazzimart.model.Product;
import com.fazzimart.model.User;
import com.fazzimart.util.DBConnection;

@Service
public class OrderService {

    private final OrderDao orderDao;
    private final CartDao cartDao;
    private final ProductDao productDao;

    public OrderService(OrderDao orderDao, CartDao cartDao, ProductDao productDao) {
        this.orderDao = orderDao;
        this.cartDao = cartDao;
        this.productDao = productDao;
    }

    public OrderDTO placeOrder(User user, CheckoutRequest request) {
        // The whole order is one MySQL transaction: stock checks, the order
        // write, stock decrements and the cart clear all share a single
        // connection (see DBConnection.beginTransaction / commitTransaction),
        // so no two requests can interleave and a failure rolls everything back.
        try {
            DBConnection.beginTransaction();
        } catch (java.sql.SQLException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not start database transaction: " + e.getMessage());
        }

        try {
            List<CartItem> cartItems = cartDao.findByUserId(user.getId());
            if (cartItems.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Your cart is empty");
            }

            Order order = new Order();
            order.setUserId(user.getId());
            order.setCustomerName(request.customerName().trim());
            order.setPhone(request.phone().trim());
            order.setAddress(request.address().trim());
            order.setCity(request.city().trim());
            order.setPostalCode(request.postalCode().trim());
            order.setStatus("PLACED");
            order.setOrderDate(LocalDateTime.now());

            BigDecimal total = BigDecimal.ZERO;

            for (CartItem cartItem : cartItems) {
                Product product = productDao.findById(cartItem.getProductId());
                if (product == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "A product in your cart no longer exists");
                }
                if (product.getStock() < cartItem.getQuantity()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Insufficient stock for \"" + product.getName() + "\" (only "
                                    + product.getStock() + " available)");
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getName());
                orderItem.setPrice(product.getPrice());
                orderItem.setQuantity(cartItem.getQuantity());
                order.addItem(orderItem);

                total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

                product.setStock(product.getStock() - cartItem.getQuantity());
                productDao.update(product);
            }

            order.setTotalAmount(total);
            Order saved = orderDao.save(order);

            cartDao.clear(user.getId());

            DBConnection.commitTransaction();
            return OrderDTO.from(saved);
        } catch (ApiException e) {
            rollbackQuietly();
            throw e;
        } catch (Exception e) {
            rollbackQuietly();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not place your order. " + e.getMessage());
        }
    }

    /** Closes the current transaction by rolling back (never throws to the caller). */
    private void rollbackQuietly() {
        try {
            DBConnection.rollbackTransaction();
        } catch (SQLException ignored) {
            // nothing sensible to do - the original error is what matters
        }
    }

    public List<OrderDTO> getOrdersForUser(User user) {
        return orderDao.findByUserId(user.getId()).stream()
                .map(OrderDTO::from)
                .toList();
    }

    public OrderDTO getOrderForUser(Long orderId, User user) {
        Order order = orderDao.findByIdAndUserId(orderId, user.getId());
        if (order == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return OrderDTO.from(order);
    }
}