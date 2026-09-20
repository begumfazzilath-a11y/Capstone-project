package com.fazzimart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fazzimart.dto.AddToCartRequest;
import com.fazzimart.dto.CartResponse;
import com.fazzimart.dto.MessageResponse;
import com.fazzimart.dto.UpdateCartRequest;
import com.fazzimart.entity.User;
import com.fazzimart.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart(currentUser()));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(currentUser(), request));
    }

    @PutMapping("/update/{productId}")
    public ResponseEntity<CartResponse> updateCart(@PathVariable Long productId,
                                                   @Valid @RequestBody UpdateCartRequest request) {
        return ResponseEntity.ok(cartService.updateCart(currentUser(), productId, request));
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<CartResponse> removeFromCart(@PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeFromCart(currentUser(), productId));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<MessageResponse> clearCart() {
        cartService.clearCart(currentUser().getId());
        return ResponseEntity.ok(new MessageResponse("Cart cleared"));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}