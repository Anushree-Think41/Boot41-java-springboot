package com.authboilerplate.service;

import com.authboilerplate.dto.AuthResponse;
import com.authboilerplate.dto.RegisterRequest;
import com.authboilerplate.entity.User;
import com.authboilerplate.exception.UserAlreadyExistsException;
import com.authboilerplate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing registration for username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(
                    "Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", request.getUsername());

        return AuthResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .message("User registered successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(String username) {
        log.debug("Fetching profile for username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.authboilerplate.exception.UserNotFoundException(
                        "User not found: " + username));

        return AuthResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .message("Authenticated successfully")
                .build();
    }
}
