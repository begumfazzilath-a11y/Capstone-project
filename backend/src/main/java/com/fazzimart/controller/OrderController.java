package com.fazzimart.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fazzimart.dto.CheckoutRequest;
import com.fazzimart.dto.OrderDTO;
import com.fazzimart.entity.User;
import com.fazzimart.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDTO> placeOrder(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(currentUser(), request));
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> myOrders() {
        return ResponseEntity.ok(orderService.getOrdersForUser(currentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderForUser(id, currentUser()));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}