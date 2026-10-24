package com.jsmc.spacemap.project.service;

import com.jsmc.spacemap.project.dto.request.LoginRequest;
import com.jsmc.spacemap.project.dto.request.RegisterRequest;
import com.jsmc.spacemap.project.dto.response.AuthResponse;
import com.jsmc.spacemap.project.entity.User;
import com.jsmc.spacemap.project.mapper.UserMapper;
import com.jsmc.spacemap.project.repository.UserRepository;
import com.jsmc.spacemap.project.service.exception.ConflictException;
import com.jsmc.spacemap.project.service.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Пользователь с таким email уже зарегистрирован");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole("ORGANIZER");
        user = userRepository.save(user);
        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Неверный email или пароль"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Неверный email или пароль");
        }
        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshForUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getEmail(), user.getRole()), UserMapper.toResponse(user));
    }
}
