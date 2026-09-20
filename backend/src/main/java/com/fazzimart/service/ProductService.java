package com.fazzimart.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fazzimart.dto.ProductDTO;
import com.fazzimart.entity.Product;
import com.fazzimart.exception.ApiException;
import com.fazzimart.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDTO> searchProducts(String category, String sort, String query) {
        List<Product> products = productRepository.findAll();

        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(q)
                            || p.getDescription().toLowerCase().contains(q))
                    .toList();
        }

        if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)) {
            String cat = category.trim();
            products = products.stream()
                    .filter(p -> p.getCategory().equalsIgnoreCase(cat))
                    .toList();
        }

        if (sort != null && !sort.isBlank()) {
            products = sortProducts(products, sort);
        }

        return products.stream().map(ProductDTO::from).toList();
    }

    public List<ProductDTO> getFeaturedProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getRating).reversed())
                .limit(8)
                .map(ProductDTO::from)
                .toList();
    }

    public ProductDTO getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        return ProductDTO.from(product);
    }

    public ProductDTO createProduct(ProductDTO dto) {
        if (productRepository.existsByName(dto.name().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A product with this name already exists");
        }
        Product product = new Product();
        applyDto(product, dto);
        return ProductDTO.from(productRepository.save(product));
    }

    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        applyDto(product, dto);
        return ProductDTO.from(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        productRepository.delete(product);
    }

    private void applyDto(Product product, ProductDTO dto) {
        product.setName(dto.name().trim());
        product.setDescription(dto.description().trim());
        product.setCategory(dto.category().trim());
        product.setPrice(dto.price());
        product.setImageUrl(dto.imageUrl().trim());
        product.setStock(dto.stock());
        product.setRating(dto.rating() == null ? new java.math.BigDecimal("4.5") : dto.rating());
    }

    private List<Product> sortProducts(List<Product> products, String sort) {
        Comparator<Product> comparator;
        switch (sort.toLowerCase()) {
            case "price_asc" -> comparator = Comparator.comparing(Product::getPrice);
            case "price_desc" -> comparator = Comparator.comparing(Product::getPrice).reversed();
            case "name_asc" -> comparator = Comparator.comparing(p -> p.getName().toLowerCase());
            case "rating_desc" -> comparator = Comparator.comparing(Product::getRating).reversed();
            default -> comparator = Comparator.comparing(Product::getId);
        }
        return products.stream().sorted(comparator).toList();
    }
}