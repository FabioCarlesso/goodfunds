package com.goodfunds.service;

import com.goodfunds.domain.User;
import com.goodfunds.dto.RegisterRequest;
import com.goodfunds.exception.EmailAlreadyInUseException;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.UserRepository;
import com.goodfunds.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_translatesDuplicateEmailRaceToDomainException() {
        RegisterRequest request = new RegisterRequest("Fulano", "Race@Example.COM", "senha12345");

        when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha12345")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uq_users_email"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessageContaining("race@example.com");

        verify(categoryRepository, never()).saveAll(any());
    }
}
