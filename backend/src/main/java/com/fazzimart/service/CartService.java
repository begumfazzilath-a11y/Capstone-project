package com.fazzimart.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fazzimart.dto.AddToCartRequest;
import com.fazzimart.dto.CartItemDTO;
import com.fazzimart.dto.CartResponse;
import com.fazzimart.dto.UpdateCartRequest;
import com.fazzimart.entity.CartItem;
import com.fazzimart.entity.Product;
import com.fazzimart.entity.User;
import com.fazzimart.exception.ApiException;
import com.fazzimart.repository.CartItemRepository;
import com.fazzimart.repository.ProductRepository;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(User user) {
        return buildCartResponse(user.getId());
    }

    @Transactional
    public CartResponse addToCart(User user, AddToCartRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());
        int newQuantity = request.quantity();
        if (existing.isPresent()) {
            newQuantity = existing.get().getQuantity() + request.quantity();
        }
        if (newQuantity > product.getStock()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Only " + product.getStock() + " units of \"" + product.getName() + "\" available");
        }

        if (existing.isPresent()) {
            existing.get().setQuantity(newQuantity);
            cartItemRepository.save(existing.get());
        } else {
            cartItemRepository.save(new CartItem(user, product, request.quantity()));
        }
        return buildCartResponse(user.getId());
    }

    @Transactional
    public CartResponse updateCart(User user, Long productId, UpdateCartRequest request) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found in cart"));

        Product product = item.getProduct();
        if (request.quantity() > product.getStock()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Only " + product.getStock() + " units of \"" + product.getName() + "\" available");
        }
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return buildCartResponse(user.getId());
    }

    @Transactional
    public CartResponse removeFromCart(User user, Long productId) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found in cart"));
        cartItemRepository.delete(item);
        return buildCartResponse(user.getId());
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    private CartResponse buildCartResponse(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        List<CartItemDTO> dtos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int itemCount = 0;

        for (CartItem item : cartItems) {
            Product p = item.getProduct();
            BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);
            itemCount += item.getQuantity();
            dtos.add(new CartItemDTO(
                    p.getId(),
                    p.getName(),
                    p.getDescription(),
                    p.getCategory(),
                    p.getPrice(),
                    p.getImageUrl(),
                    item.getQuantity(),
                    subtotal,
                    p.getStock()));
        }
        return new CartResponse(dtos, total, itemCount);
    }
}