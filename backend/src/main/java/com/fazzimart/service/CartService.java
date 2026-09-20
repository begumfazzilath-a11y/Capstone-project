package com.fazzimart.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fazzimart.dao.CartDao;
import com.fazzimart.dao.ProductDao;
import com.fazzimart.dto.AddToCartRequest;
import com.fazzimart.dto.CartItemDTO;
import com.fazzimart.dto.CartResponse;
import com.fazzimart.dto.UpdateCartRequest;
import com.fazzimart.exception.ApiException;
import com.fazzimart.model.CartItem;
import com.fazzimart.model.Product;
import com.fazzimart.model.User;

@Service
public class CartService {

    private final CartDao cartDao;
    private final ProductDao productDao;

    public CartService(CartDao cartDao, ProductDao productDao) {
        this.cartDao = cartDao;
        this.productDao = productDao;
    }

    public CartResponse getCart(User user) {
        List<CartItem> cartItems = cartDao.findByUserId(user.getId());
        List<CartItemDTO> dtos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int itemCount = 0;

        for (CartItem item : cartItems) {
            Product p = productDao.findById(item.getProductId());
            if (p == null) {
                continue;
            }
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

    public CartResponse addToCart(User user, AddToCartRequest request) {
        Product product = productDao.findById(request.productId());
        if (product == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found");
        }

        CartItem existing = cartDao.findByUserIdAndProductId(user.getId(), product.getId());
        int newQuantity = request.quantity();
        if (existing != null) {
            newQuantity = existing.getQuantity() + request.quantity();
        }
        if (newQuantity > product.getStock()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Only " + product.getStock() + " units of \"" + product.getName() + "\" available");
        }

        cartDao.addOrUpdate(user.getId(), product.getId(), newQuantity);
        return getCart(user);
    }

    public CartResponse updateCart(User user, Long productId, UpdateCartRequest request) {
        CartItem item = cartDao.findByUserIdAndProductId(user.getId(), productId);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Item not found in cart");
        }

        Product product = productDao.findById(productId);
        if (product != null && request.quantity() > product.getStock()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Only " + product.getStock() + " units of \"" + product.getName() + "\" available");
        }

        cartDao.setQuantity(user.getId(), productId, request.quantity());
        return getCart(user);
    }

    public CartResponse removeFromCart(User user, Long productId) {
        CartItem item = cartDao.findByUserIdAndProductId(user.getId(), productId);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Item not found in cart");
        }
        cartDao.remove(user.getId(), productId);
        return getCart(user);
    }

    public void clearCart(Long userId) {
        cartDao.clear(userId);
    }
}