package com.fazzimart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fazzimart.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
}