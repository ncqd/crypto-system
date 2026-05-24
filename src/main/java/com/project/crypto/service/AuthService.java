package com.project.crypto.service;

import com.project.crypto.domain.entity.User;
import com.project.crypto.dto.AuthResponse;
import com.project.crypto.dto.LoginRequest;
import com.project.crypto.dto.RegisterRequest;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.repository.UserRepository;
import com.project.crypto.security.JwtService;
import com.project.crypto.support.AppLog;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WalletSetupService walletSetupService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            WalletSetupService walletSetupService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.walletSetupService = walletSetupService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username already taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedAt(Instant.now());
        User saved = userRepository.save(user);

        walletSetupService.createWalletsForUser(saved);

        String token = jwtService.createToken(saved);
        AppLog.info(
                log,
                AuthService.class,
                "register",
                "User id=%s username=%s wallets=USDT,ETH,BTC".formatted(saved.getId(), saved.getUsername()));

        return AuthResponse.of(token, saved.getId(), saved.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByUsername(request.username().trim())
                .orElseThrow(() -> new BusinessException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid username or password");
        }

        String token = jwtService.createToken(user);
        AppLog.info(
                log,
                AuthService.class,
                "login",
                "User id=%s username=%s".formatted(user.getId(), user.getUsername()));

        return AuthResponse.of(token, user.getId(), user.getUsername());
    }
}
