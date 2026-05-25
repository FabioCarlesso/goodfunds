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
import com.goodfunds.service.ReportCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que {@code GET /reports/summary} e cacheado (Caffeine) e que a invalidacao
 * por usuario funciona: escritas via API limpam o cache do dono, escritas diretas no
 * banco nao (provando que o cache esta ativo) e o cache de um usuario nao e descartado
 * pela escrita de outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportCacheIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private ReportCacheService reportCacheService;

    private User owner;
    private User other;
    private String ownerToken;
    private String otherToken;
    private Category ownerDespesa;
    private Category otherDespesa;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        invoiceRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .nome("Owner")
                .email("owner-cache@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());
        other = userRepository.save(User.builder()
                .nome("Other")
                .email("other-cache@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());

        ownerToken = jwtService.generateToken(owner.getEmail());
        otherToken = jwtService.generateToken(other.getEmail());

        ownerDespesa = persistCategory(owner, "Alimentacao", TipoCategoria.DESPESA);
        otherDespesa = persistCategory(other, "Alien", TipoCategoria.DESPESA);

        // Garante que nenhuma entrada de execucoes anteriores (contexto Spring reutilizado)
        // interfira nas asserts de cache deste teste.
        reportCacheService.evictUser(owner.getId());
        reportCacheService.evictUser(other.getId());
    }

    @Test
    void summary_isCached_directDbWriteNotReflectedUntilEviction() throws Exception {
        // Primeira leitura: sem despesas. Popula o cache.
        getSummary(ownerToken).andExpect(jsonPath("$.despesas").value(0.0));

        // Escrita direta no banco contorna a invalidacao do servico.
        persistTransaction(owner, ownerDespesa, "100.00", LocalDate.of(2026, 5, 5));

        // Ainda servido do cache: a despesa nova nao aparece.
        getSummary(ownerToken).andExpect(jsonPath("$.despesas").value(0.0));

        // Apos invalidar, a leitura recomputa e reflete o banco.
        reportCacheService.evictUser(owner.getId());
        getSummary(ownerToken).andExpect(jsonPath("$.despesas").value(100.00));
    }

    @Test
    void summary_createTransactionViaApi_invalidatesCache() throws Exception {
        getSummary(ownerToken).andExpect(jsonPath("$.despesas").value(0.0));

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson(ownerDespesa, "250.00", "2026-05-10")))
                .andExpect(status().isCreated());

        getSummary(ownerToken).andExpect(jsonPath("$.despesas").value(250.00));
    }

    @Test
    void summary_evictionIsScopedPerUser() throws Exception {
        // Popula o cache de ambos os usuarios.
        getSummary(ownerToken).andExpect(jsonPath("$.despesas").value(0.0));
        getSummary(otherToken).andExpect(jsonPath("$.despesas").value(0.0));

        // Escritas diretas (sem invalidacao) para ambos.
        persistTransaction(owner, ownerDespesa, "100.00", LocalDate.of(2026, 5, 5));
        persistTransaction(other, otherDespesa, "200.00", LocalDate.of(2026, 5, 5));

        // Invalida apenas o dono.
        reportCacheService.evictUser(owner.getId());

        // Dono recomputa; o outro continua servido do cache antigo.
        getSummary(ownerToken).andExpect(jsonPath("$.despesas").value(100.00));
        getSummary(otherToken).andExpect(jsonPath("$.despesas").value(0.0));
    }

    private org.springframework.test.web.servlet.ResultActions getSummary(String token) throws Exception {
        return mockMvc.perform(get("/reports/summary")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String transactionJson(Category category, String valor, String data) {
        return """
                {
                  "descricao": "lancamento",
                  "valor": %s,
                  "data": "%s",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(valor, data, category.getId());
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
