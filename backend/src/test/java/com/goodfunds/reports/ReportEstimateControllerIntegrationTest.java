package com.goodfunds.reports;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.FormaPagamento;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.Transaction;
import com.goodfunds.domain.User;
import com.goodfunds.repository.BudgetRepository;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.InvoiceRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
import com.goodfunds.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportEstimateControllerIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        // Congela "hoje" em 22/05/2026 (maio: 31 dias, 22 decorridos).
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-05-22T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User owner;
    private User other;
    private String ownerToken;
    private String otherToken;
    private Category alimentacao;
    private Category transporte;
    private Category alienCategory;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        invoiceRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .nome("Owner")
                .email("owner@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());
        other = userRepository.save(User.builder()
                .nome("Other")
                .email("other@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());

        ownerToken = jwtService.generateToken(owner.getEmail());
        otherToken = jwtService.generateToken(other.getEmail());

        alimentacao = persistCategory(owner, "Alimentacao");
        transporte = persistCategory(owner, "Transporte");
        alienCategory = persistCategory(other, "Alien");
    }

    @Test
    void estimate_returnsConsolidatedAndPerCategoryProjection() throws Exception {
        // Alimentacao: 300 em cada um dos 3 meses fechados -> media 300; 220 no mes corrente.
        persistTransaction(owner, alimentacao, "300.00", LocalDate.of(2026, 2, 15));
        persistTransaction(owner, alimentacao, "300.00", LocalDate.of(2026, 3, 15));
        persistTransaction(owner, alimentacao, "300.00", LocalDate.of(2026, 4, 15));
        persistTransaction(owner, alimentacao, "220.00", LocalDate.of(2026, 5, 10));
        // Transporte: historico parcial (so abril) -> media 50; sem lancamento no mes corrente.
        persistTransaction(owner, transporte, "150.00", LocalDate.of(2026, 4, 20));
        // Dados de outro usuario nao podem vazar.
        persistTransaction(other, alienCategory, "999.00", LocalDate.of(2026, 4, 10));
        persistTransaction(other, alienCategory, "999.00", LocalDate.of(2026, 5, 5));

        mockMvc.perform(get("/reports/estimate")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ref").value("2026-05"))
                .andExpect(jsonPath("$.diasNoMes").value(31))
                .andExpect(jsonPath("$.diasDecorridos").value(22))
                .andExpect(jsonPath("$.categorias.length()").value(2))
                .andExpect(jsonPath("$.categorias[0].categoryNome").value("Alimentacao"))
                .andExpect(jsonPath("$.categorias[0].categoryTipo").value("DESPESA"))
                .andExpect(jsonPath("$.categorias[0].media").value(300.00))
                .andExpect(jsonPath("$.categorias[0].realizado").value(220.00))
                .andExpect(jsonPath("$.categorias[0].projecao").value(310.00))
                .andExpect(jsonPath("$.categorias[1].categoryNome").value("Transporte"))
                .andExpect(jsonPath("$.categorias[1].media").value(50.00))
                .andExpect(jsonPath("$.categorias[1].realizado").value(0.0))
                .andExpect(jsonPath("$.categorias[1].projecao").value(0.0))
                .andExpect(jsonPath("$.consolidado.media").value(350.00))
                .andExpect(jsonPath("$.consolidado.realizado").value(220.00))
                .andExpect(jsonPath("$.consolidado.projecao").value(310.00));
    }

    @Test
    void estimate_userWithoutData_returnsEmptyAndZeroTotals() throws Exception {
        mockMvc.perform(get("/reports/estimate")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ref").value("2026-05"))
                .andExpect(jsonPath("$.categorias.length()").value(0))
                .andExpect(jsonPath("$.consolidado.media").value(0.0))
                .andExpect(jsonPath("$.consolidado.realizado").value(0.0))
                .andExpect(jsonPath("$.consolidado.projecao").value(0.0));
    }

    @Test
    void estimate_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/reports/estimate"))
                .andExpect(status().isUnauthorized());
    }

    private Category persistCategory(User user, String nome) {
        return categoryRepository.save(Category.builder()
                .nome(nome)
                .tipo(TipoCategoria.DESPESA)
                .user(user)
                .build());
    }

    private Transaction persistTransaction(User user, Category category, String valor, LocalDate data) {
        return transactionRepository.save(Transaction.builder()
                .descricao("lancamento")
                .valor(new BigDecimal(valor))
                .data(data)
                .formaPagamento(FormaPagamento.CARTAO_CREDITO)
                .category(category)
                .user(user)
                .build());
    }
}
