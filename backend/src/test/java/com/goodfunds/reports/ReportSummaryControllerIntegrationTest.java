package com.goodfunds.reports;

import com.goodfunds.domain.Budget;
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
class ReportSummaryControllerIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        // Congela "hoje" em 2026-05-22 para que o default de ref seja maio/2026.
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
    private Category salario;
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
                .email("owner-summary@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());
        other = userRepository.save(User.builder()
                .nome("Other")
                .email("other-summary@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());

        ownerToken = jwtService.generateToken(owner.getEmail());
        otherToken = jwtService.generateToken(other.getEmail());

        alimentacao = persistCategory(owner, "Alimentacao", TipoCategoria.DESPESA);
        transporte = persistCategory(owner, "Transporte", TipoCategoria.DESPESA);
        salario = persistCategory(owner, "Salario", TipoCategoria.RECEITA);
        alienCategory = persistCategory(other, "Alien", TipoCategoria.DESPESA);
    }

    @Test
    void summary_returnsTotalsForRef() throws Exception {
        // Despesas: 150 + 80 = 230 em maio/2026
        persistTransaction(owner, alimentacao, "150.00", LocalDate.of(2026, 5, 5));
        persistTransaction(owner, transporte, "80.00", LocalDate.of(2026, 5, 10));
        // Receita: 3000 em maio/2026
        persistTransaction(owner, salario, "3000.00", LocalDate.of(2026, 5, 1));
        // Orcamento: 500 para alimentacao
        persistBudget(owner, alimentacao, "500.00", 2026, 5);
        // Transacoes fora do mes — nao devem entrar no resumo
        persistTransaction(owner, alimentacao, "999.00", LocalDate.of(2026, 4, 30));
        persistTransaction(owner, alimentacao, "999.00", LocalDate.of(2026, 6, 1));
        // Dados de outro usuario — isolamento
        persistTransaction(other, alienCategory, "555.00", LocalDate.of(2026, 5, 15));

        mockMvc.perform(get("/reports/summary")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ref").value("2026-05"))
                .andExpect(jsonPath("$.receitas").value(3000.00))
                .andExpect(jsonPath("$.despesas").value(230.00))
                .andExpect(jsonPath("$.orcado").value(500.00))
                .andExpect(jsonPath("$.saldo").value(2770.00))
                .andExpect(jsonPath("$.percentualOrcadoUsado").value(46.00));
    }

    @Test
    void summary_defaultRef_usesCurrentMonth() throws Exception {
        // Sem transacoes nem orcamentos: espera zeros para o mes corrente (maio/2026 via clock fixo)
        mockMvc.perform(get("/reports/summary")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ref").value("2026-05"))
                .andExpect(jsonPath("$.receitas").value(0.0))
                .andExpect(jsonPath("$.despesas").value(0.0))
                .andExpect(jsonPath("$.orcado").value(0.0))
                .andExpect(jsonPath("$.saldo").value(0.0))
                .andExpect(jsonPath("$.percentualOrcadoUsado").value(0.0));
    }

    @Test
    void summary_noTransactionsNoBudget_returnsAllZeros() throws Exception {
        mockMvc.perform(get("/reports/summary")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receitas").value(0.0))
                .andExpect(jsonPath("$.despesas").value(0.0))
                .andExpect(jsonPath("$.orcado").value(0.0))
                .andExpect(jsonPath("$.saldo").value(0.0))
                .andExpect(jsonPath("$.percentualOrcadoUsado").value(0.0));
    }

    @Test
    void summary_noBudget_percentualIsZero() throws Exception {
        persistTransaction(owner, alimentacao, "200.00", LocalDate.of(2026, 5, 5));

        mockMvc.perform(get("/reports/summary")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.despesas").value(200.00))
                .andExpect(jsonPath("$.orcado").value(0.0))
                .andExpect(jsonPath("$.percentualOrcadoUsado").value(0.0));
    }

    @Test
    void summary_userIsolation_otherUserSeesOnlyOwnData() throws Exception {
        persistTransaction(owner, alimentacao, "300.00", LocalDate.of(2026, 5, 5));
        persistTransaction(other, alienCategory, "500.00", LocalDate.of(2026, 5, 5));

        mockMvc.perform(get("/reports/summary")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.despesas").value(500.00))
                .andExpect(jsonPath("$.receitas").value(0.0));
    }

    @Test
    void summary_invalidRefFormat_returns400() throws Exception {
        mockMvc.perform(get("/reports/summary")
                        .param("ref", "abc")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summary_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/reports/summary").param("ref", "2026-05"))
                .andExpect(status().isUnauthorized());
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

    private Budget persistBudget(User user, Category category, String limite, Integer ano, Integer mes) {
        return budgetRepository.save(Budget.builder()
                .limite(new BigDecimal(limite))
                .category(category)
                .ano(ano)
                .mes(mes)
                .user(user)
                .build());
    }
}
