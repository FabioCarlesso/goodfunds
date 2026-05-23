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
class ReportByCategoryControllerIntegrationTest {

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
                .email("owner-bycat@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());
        other = userRepository.save(User.builder()
                .nome("Other")
                .email("other-bycat@example.com")
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
    void byCategory_returnsTotalsForRef() throws Exception {
        // Alimentacao: 100 + 50 = 150 em maio/2026
        persistTransaction(owner, alimentacao, "100.00", LocalDate.of(2026, 5, 5));
        persistTransaction(owner, alimentacao, "50.00", LocalDate.of(2026, 5, 20));
        // Transporte: 80 em maio/2026
        persistTransaction(owner, transporte, "80.00", LocalDate.of(2026, 5, 10));
        // Salario: 3000 em maio/2026 (receita)
        persistTransaction(owner, salario, "3000.00", LocalDate.of(2026, 5, 1));
        // Transacoes fora do mes de referencia — nao devem aparecer
        persistTransaction(owner, alimentacao, "999.00", LocalDate.of(2026, 4, 30));
        persistTransaction(owner, alimentacao, "999.00", LocalDate.of(2026, 6, 1));
        // Dados de outro usuario — nao devem aparecer
        persistTransaction(other, alienCategory, "555.00", LocalDate.of(2026, 5, 15));

        mockMvc.perform(get("/reports/by-category")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nome").value("Alimentacao"))
                .andExpect(jsonPath("$[0].tipo").value("DESPESA"))
                .andExpect(jsonPath("$[0].total").value(150.00))
                .andExpect(jsonPath("$[1].nome").value("Salario"))
                .andExpect(jsonPath("$[1].tipo").value("RECEITA"))
                .andExpect(jsonPath("$[1].total").value(3000.00))
                .andExpect(jsonPath("$[2].nome").value("Transporte"))
                .andExpect(jsonPath("$[2].tipo").value("DESPESA"))
                .andExpect(jsonPath("$[2].total").value(80.00));
    }

    @Test
    void byCategory_monthWithNoTransactions_returnsEmptyList() throws Exception {
        persistTransaction(owner, alimentacao, "100.00", LocalDate.of(2026, 4, 10));

        mockMvc.perform(get("/reports/by-category")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void byCategory_userIsolation_otherUserSeesOnlyOwnData() throws Exception {
        persistTransaction(owner, alimentacao, "100.00", LocalDate.of(2026, 5, 5));
        persistTransaction(other, alienCategory, "500.00", LocalDate.of(2026, 5, 5));

        mockMvc.perform(get("/reports/by-category")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Alien"))
                .andExpect(jsonPath("$[0].total").value(500.00));
    }

    @Test
    void byCategory_missingRef_returns400() throws Exception {
        mockMvc.perform(get("/reports/by-category")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void byCategory_invalidRefFormat_returns400() throws Exception {
        mockMvc.perform(get("/reports/by-category")
                        .param("ref", "abc")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void byCategory_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/reports/by-category").param("ref", "2026-05"))
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
}
