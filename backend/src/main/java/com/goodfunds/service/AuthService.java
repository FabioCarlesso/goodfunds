package com.goodfunds.service;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.User;
import com.goodfunds.dto.AuthResponse;
import com.goodfunds.dto.LoginRequest;
import com.goodfunds.dto.RegisterRequest;
import com.goodfunds.exception.EmailAlreadyInUseException;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.UserRepository;
import com.goodfunds.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new DefaultCategory("Alimentacao", TipoCategoria.DESPESA),
            new DefaultCategory("Transporte", TipoCategoria.DESPESA),
            new DefaultCategory("Moradia", TipoCategoria.DESPESA),
            new DefaultCategory("Lazer", TipoCategoria.DESPESA),
            new DefaultCategory("Saude", TipoCategoria.DESPESA),
            new DefaultCategory("Educacao", TipoCategoria.DESPESA),
            new DefaultCategory("Salario", TipoCategoria.RECEITA),
            new DefaultCategory("Outros", TipoCategoria.DESPESA)
    );

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       CategoryRepository categoryRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email);
        }

        User user = User.builder()
                .nome(request.nome().trim())
                .email(email)
                .senha(passwordEncoder.encode(request.senha()))
                .build();
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyInUseException(email);
        }

        seedDefaultCategories(user);

        String token = jwtService.generateToken(user.getEmail());
        return AuthResponse.bearer(token, jwtService.getExpirationMillis());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.senha()));

        String token = jwtService.generateToken(authentication.getName());
        return AuthResponse.bearer(token, jwtService.getExpirationMillis());
    }

    private void seedDefaultCategories(User user) {
        List<Category> seeds = DEFAULT_CATEGORIES.stream()
                .map(def -> Category.builder()
                        .nome(def.nome())
                        .tipo(def.tipo())
                        .user(user)
                        .build())
                .toList();
        categoryRepository.saveAll(seeds);
    }

    private record DefaultCategory(String nome, TipoCategoria tipo) {
    }
}
