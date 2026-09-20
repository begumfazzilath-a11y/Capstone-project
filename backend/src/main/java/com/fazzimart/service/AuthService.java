package com.fazzimart.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fazzimart.dao.UserDao;
import com.fazzimart.dto.AuthResponse;
import com.fazzimart.dto.LoginRequest;
import com.fazzimart.dto.RegisterRequest;
import com.fazzimart.dto.UserDTO;
import com.fazzimart.exception.ApiException;
import com.fazzimart.model.User;
import com.fazzimart.security.JwtTokenProvider;

@Service
public class AuthService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserDao userDao,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userDao.existsByEmail(request.email().trim().toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is already registered");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setPhone(request.phone().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        userDao.save(user);

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, "Bearer", UserDTO.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userDao.findByEmail(request.email().trim().toLowerCase());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, "Bearer", UserDTO.from(user));
    }
}