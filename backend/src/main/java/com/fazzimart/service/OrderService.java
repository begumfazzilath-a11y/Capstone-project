package com.fazzimart.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fazzimart.dto.CheckoutRequest;
import com.fazzimart.dto.OrderDTO;
import com.fazzimart.entity.CartItem;
import com.fazzimart.entity.Order;
import com.fazzimart.entity.OrderItem;
import com.fazzimart.entity.Product;
import com.fazzimart.entity.User;
import com.fazzimart.exception.ApiException;
import com.fazzimart.repository.CartItemRepository;
import com.fazzimart.repository.OrderRepository;
import com.fazzimart.repository.ProductRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderDTO placeOrder(User user, CheckoutRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByIdAsc(user.getId());
        if (cartItems.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setCustomerName(request.customerName().trim());
        order.setPhone(request.phone().trim());
        order.setAddress(request.address().trim());
        order.setCity(request.city().trim());
        order.setPostalCode(request.postalCode().trim());
        order.setStatus("PLACED");

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
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
            productRepository.save(product);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        cartItemRepository.deleteByUserId(user.getId());

        return OrderDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersForUser(User user) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(user.getId()).stream()
                .map(OrderDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderForUser(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view another user's order");
        }
        return OrderDTO.from(order);
    }
}