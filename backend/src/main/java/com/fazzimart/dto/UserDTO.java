package com.fazzimart.dto;

import com.fazzimart.entity.User;

public record UserDTO(Long id, String name, String phone, String email, String role) {

    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole());
    }
}