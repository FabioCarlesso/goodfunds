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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportEvolutionControllerIntegrationTest {

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
    private Category despesa;
    private Category receita;
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
                .email("owner-evo@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());
        other = userRepository.save(User.builder()
                .nome("Other")
                .email("other-evo@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());

        ownerToken = jwtService.generateToken(owner.getEmail());
        otherToken = jwtService.generateToken(other.getEmail());

        despesa = persistCategory(owner, "Alimentacao", TipoCategoria.DESPESA);
        receita = persistCategory(owner, "Salario", TipoCategoria.RECEITA);
        alienCategory = persistCategory(other, "Alien", TipoCategoria.DESPESA);
    }

    @Test
    void evolution_returnsMonthlySeriesWithAllMonthsInRange() throws Exception {
        // Jan: despesas 200, receitas 1000
        persistTransaction(owner, despesa, "200.00", LocalDate.of(2026, 1, 10));
        persistTransaction(owner, receita, "1000.00", LocalDate.of(2026, 1, 15));
        // Fev: sem transacoes -> zeros
        // Mar: despesas 350, sem receitas
        persistTransaction(owner, despesa, "150.00", LocalDate.of(2026, 3, 5));
        persistTransaction(owner, despesa, "200.00", LocalDate.of(2026, 3, 20));
        // Fora do range — nao deve aparecer
        persistTransaction(owner, despesa, "999.00", LocalDate.of(2025, 12, 31));
        persistTransaction(owner, despesa, "999.00", LocalDate.of(2026, 4, 1));
        // Dados de outro usuario — nao devem aparecer
        persistTransaction(other, alienCategory, "555.00", LocalDate.of(2026, 2, 10));

        mockMvc.perform(get("/reports/evolution")
                        .param("from", "2026-01")
                        .param("to", "2026-03")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].ref").value("2026-01"))
                .andExpect(jsonPath("$[0].receitas").value(1000.00))
                .andExpect(jsonPath("$[0].despesas").value(200.00))
                .andExpect(jsonPath("$[1].ref").value("2026-02"))
                .andExpect(jsonPath("$[1].receitas").value(0.0))
                .andExpect(jsonPath("$[1].despesas").value(0.0))
                .andExpect(jsonPath("$[2].ref").value("2026-03"))
                .andExpect(jsonPath("$[2].receitas").value(0.0))
                .andExpect(jsonPath("$[2].despesas").value(350.00));
    }

    @Test
    void evolution_singleMonth_returnsSingleEntry() throws Exception {
        persistTransaction(owner, despesa, "500.00", LocalDate.of(2026, 5, 10));
        persistTransaction(owner, receita, "2000.00", LocalDate.of(2026, 5, 1));

        mockMvc.perform(get("/reports/evolution")
                        .param("from", "2026-05")
                        .param("to", "2026-05")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ref").value("2026-05"))
                .andExpect(jsonPath("$[0].receitas").value(2000.00))
                .andExpect(jsonPath("$[0].despesas").value(500.00));
    }

    @Test
    void evolution_fromAfterTo_returns400() throws Exception {
        mockMvc.perform(get("/reports/evolution")
                        .param("from", "2026-05")
                        .param("to", "2026-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evolution_missingParams_returns400() throws Exception {
        mockMvc.perform(get("/reports/evolution")
                        .param("from", "2026-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evolution_invalidDateFormat_returns400() throws Exception {
        mockMvc.perform(get("/reports/evolution")
                        .param("from", "abc")
                        .param("to", "2026-03")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evolution_rangeExceedingMax_returns400() throws Exception {
        // 37 meses (2026-01 .. 2029-01) excede o teto de 36 meses.
        mockMvc.perform(get("/reports/evolution")
                        .param("from", "2026-01")
                        .param("to", "2029-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:invalid-filter"));
    }

    @Test
    void evolution_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/reports/evolution")
                        .param("from", "2026-01")
                        .param("to", "2026-03"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void evolution_userIsolation_otherUserSeesOnlyOwnData() throws Exception {
        persistTransaction(owner, despesa, "100.00", LocalDate.of(2026, 1, 5));
        persistTransaction(other, alienCategory, "500.00", LocalDate.of(2026, 1, 5));

        mockMvc.perform(get("/reports/evolution")
                        .param("from", "2026-01")
                        .param("to", "2026-01")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].despesas").value(500.00))
                .andExpect(jsonPath("$[0].receitas").value(0.0));
    }

    private Category persistCategory(User user, String nome, TipoCategoria tipo) {
        return categoryRepository.save(Category.builder()
                .nome(nome)
                .tipo(tipo)
                .user(user)
                .build());
    }

    private Transaction persistTransaction(User user, Category category, String valor, LocalDate data) {
        return transactionRepository.save(Transaction.builder()
                .descricao("lancamento")
                .valor(new BigDecimal(valor))
                .data(data)
                .formaPagamento(FormaPagamento.PIX)
                .category(category)
                .user(user)
                .build());
    }
}
