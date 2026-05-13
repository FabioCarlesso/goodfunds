package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.User;
import com.goodfunds.dto.AuthResponse;
import com.goodfunds.dto.RegisterRequest;
import com.goodfunds.exception.EmailAlreadyInUseException;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.UserRepository;
import com.goodfunds.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void register_seedsEightDefaultCategoriesForNewUser() {
        RegisterRequest request = new RegisterRequest("Fulano", "fulano@example.com", "senha12345");
        User savedUser = User.builder()
                .nome("Fulano")
                .email("fulano@example.com")
                .senha("encoded")
                .build();

        when(userRepository.existsByEmail("fulano@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha12345")).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken("fulano@example.com")).thenReturn("token");
        when(jwtService.getExpirationMillis()).thenReturn(86_400_000L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());

        List<Category> seeded = captor.getValue();
        assertThat(seeded).hasSize(8);

        List<String> names = seeded.stream().map(Category::getNome).toList();
        assertThat(names).containsExactlyInAnyOrder(
                "Alimentacao", "Transporte", "Moradia", "Lazer",
                "Saude", "Educacao", "Salario", "Outros");

        long despesas = seeded.stream().filter(c -> c.getTipo() == TipoCategoria.DESPESA).count();
        long receitas = seeded.stream().filter(c -> c.getTipo() == TipoCategoria.RECEITA).count();
        assertThat(despesas).isEqualTo(7);
        assertThat(receitas).isEqualTo(1);

        assertThat(seeded.stream().filter(c -> "Salario".equals(c.getNome())).findFirst())
                .get().extracting(Category::getTipo).isEqualTo(TipoCategoria.RECEITA);
    }

    @Test
    void register_allCategoriesAreLinkedToTheNewUser() {
        RegisterRequest request = new RegisterRequest("Fulano", "fulano@example.com", "senha12345");
        User savedUser = User.builder()
                .nome("Fulano")
                .email("fulano@example.com")
                .senha("encoded")
                .build();

        when(userRepository.existsByEmail("fulano@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha12345")).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken("fulano@example.com")).thenReturn("token");
        when(jwtService.getExpirationMillis()).thenReturn(86_400_000L);

        authService.register(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .allSatisfy(c -> assertThat(c.getUser()).isSameAs(savedUser));
    }

    @Test
    void register_withKnownDuplicateEmail_doesNotSeedCategories() {
        RegisterRequest request = new RegisterRequest("Fulano", "dup@example.com", "senha12345");
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyInUseException.class);

        verify(categoryRepository, never()).saveAll(any());
    }

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
