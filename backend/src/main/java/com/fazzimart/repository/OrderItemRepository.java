package com.fazzimart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fazzimart.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}