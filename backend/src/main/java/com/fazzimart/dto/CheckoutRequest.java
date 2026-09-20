package com.fazzimart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 100, message = "Name must be 100 characters or less")
        String customerName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "Enter a valid phone number")
        String phone,

        @NotBlank(message = "Delivery address is required")
        @Size(max = 255, message = "Address must be 255 characters or less")
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be 100 characters or less")
        String city,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must be 20 characters or less")
        String postalCode) {
}