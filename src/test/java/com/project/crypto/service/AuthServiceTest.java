package com.project.crypto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.crypto.domain.entity.User;
import com.project.crypto.dto.LoginRequest;
import com.project.crypto.dto.RegisterRequest;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.repository.UserRepository;
import com.project.crypto.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private WalletSetupService walletSetupService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
    }

    @Test
    void register_createsUserWalletsAndToken() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.createToken(any(User.class))).thenReturn("jwt-token");

        var response = authService.register(new RegisterRequest("alice", "secret123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed");
        verify(walletSetupService).createWalletsForUser(any(User.class));
    }

    @Test
    void register_rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "secret123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already taken");

        verify(walletSetupService, never()).createWalletsForUser(any());
    }

    @Test
    void login_returnsTokenWhenPasswordMatches() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.createToken(user)).thenReturn("jwt-token");

        var response = authService.login(new LoginRequest("bob", "secret123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(2L);
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = new User();
        user.setUsername("bob");
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("wrong"), eq("hashed"))).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
