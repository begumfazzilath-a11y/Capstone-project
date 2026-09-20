package com.fazzimart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fazzimart.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByName(String name);
}