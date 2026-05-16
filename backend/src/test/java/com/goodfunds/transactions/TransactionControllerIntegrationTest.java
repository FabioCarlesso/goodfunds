package com.goodfunds.transactions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goodfunds.domain.Category;
import com.goodfunds.domain.FormaPagamento;
import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.domain.StatusFatura;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User owner;
    private User other;
    private Category alimentacao;
    private Category lazer;
    private Category salario;
    private Category otherUserCategory;
    private String ownerToken;
    private String otherToken;

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

        alimentacao = categoryRepository.save(Category.builder()
                .nome("Alimentacao").tipo(TipoCategoria.DESPESA).user(owner).build());
        lazer = categoryRepository.save(Category.builder()
                .nome("Lazer").tipo(TipoCategoria.DESPESA).user(owner).build());
        salario = categoryRepository.save(Category.builder()
                .nome("Salario").tipo(TipoCategoria.RECEITA).user(owner).build());
        otherUserCategory = categoryRepository.save(Category.builder()
                .nome("Outros").tipo(TipoCategoria.DESPESA).user(other).build());

        ownerToken = jwtService.generateToken(owner.getEmail());
        otherToken = jwtService.generateToken(other.getEmail());
    }

    @Test
    void create_persistsTransactionAndReturns201() throws Exception {
        String payload = """
                {
                  "descricao": "Mercado",
                  "valor": 125.50,
                  "data": "2026-05-10",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(alimentacao.getId());

        MvcResult result = mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Mercado"))
                .andExpect(jsonPath("$.valor").value(125.50))
                .andExpect(jsonPath("$.data").value("2026-05-10"))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.categoryId").value(alimentacao.getId().toString()))
                .andExpect(jsonPath("$.categoryNome").value("Alimentacao"))
                .andExpect(jsonPath("$.categoryTipo").value("DESPESA"))
                .andExpect(jsonPath("$.invoiceId").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID createdId = UUID.fromString(body.get("id").asText());

        Transaction persisted = transactionRepository.findById(createdId).orElseThrow();
        assertThat(persisted.getUser().getId()).isEqualTo(owner.getId());
        assertThat(persisted.getCategory().getId()).isEqualTo(alimentacao.getId());
    }

    @Test
    void create_withNegativeValor_returns400() throws Exception {
        String payload = """
                {
                  "descricao": "Invalida",
                  "valor": -10.00,
                  "data": "2026-05-10",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.valor").exists());
    }

    @Test
    void create_withMissingFields_returns400() throws Exception {
        String payload = """
                {
                  "descricao": "",
                  "valor": null,
                  "data": null,
                  "formaPagamento": null,
                  "categoryId": null
                }
                """;

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void create_withCategoryFromAnotherUser_returns404() throws Exception {
        String payload = """
                {
                  "descricao": "Tentativa",
                  "valor": 10.00,
                  "data": "2026-05-10",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(otherUserCategory.getId());

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-not-found"))
                .andExpect(jsonPath("$.title").value("Categoria nao encontrada"));
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        String payload = """
                {
                  "descricao": "x",
                  "valor": 10.00,
                  "data": "2026-05-10",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsEmptyPageWhenUserHasNoTransactions() throws Exception {
        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void list_returnsOnlyOwnerTransactions() throws Exception {
        persistTransaction(owner, alimentacao, "Mercado", "100.00",
                LocalDate.of(2026, 5, 10), FormaPagamento.PIX);
        persistTransaction(other, otherUserCategory, "Nao deve aparecer", "999.00",
                LocalDate.of(2026, 5, 10), FormaPagamento.PIX);

        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].descricao").value("Mercado"));
    }

    @Test
    void list_filterByRef_returnsOnlyMonthOfReference() throws Exception {
        persistTransaction(owner, alimentacao, "Maio", "10.00",
                LocalDate.of(2026, 5, 15), FormaPagamento.PIX);
        persistTransaction(owner, alimentacao, "Abril", "20.00",
                LocalDate.of(2026, 4, 15), FormaPagamento.PIX);

        mockMvc.perform(get("/transactions")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].descricao").value("Maio"));
    }

    @Test
    void list_filterByCategoryId_returnsOnlyMatchingCategory() throws Exception {
        persistTransaction(owner, alimentacao, "Mercado", "50.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);
        persistTransaction(owner, lazer, "Cinema", "40.00",
                LocalDate.of(2026, 5, 2), FormaPagamento.DINHEIRO);

        mockMvc.perform(get("/transactions")
                        .param("categoryId", lazer.getId().toString())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].descricao").value("Cinema"));
    }

    @Test
    void list_filterByTipo_returnsOnlyMatchingTipo() throws Exception {
        persistTransaction(owner, alimentacao, "Despesa", "50.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);
        persistTransaction(owner, salario, "Receita", "3000.00",
                LocalDate.of(2026, 5, 5), FormaPagamento.TRANSFERENCIA);

        mockMvc.perform(get("/transactions")
                        .param("tipo", "RECEITA")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].descricao").value("Receita"))
                .andExpect(jsonPath("$.content[0].categoryTipo").value("RECEITA"));
    }

    @Test
    void list_filterByFromAndTo_returnsOnlyInRange() throws Exception {
        persistTransaction(owner, alimentacao, "Antes", "10.00",
                LocalDate.of(2026, 4, 30), FormaPagamento.PIX);
        persistTransaction(owner, alimentacao, "Dentro", "20.00",
                LocalDate.of(2026, 5, 10), FormaPagamento.PIX);
        persistTransaction(owner, alimentacao, "Depois", "30.00",
                LocalDate.of(2026, 6, 1), FormaPagamento.PIX);

        mockMvc.perform(get("/transactions")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].descricao").value("Dentro"));
    }

    @Test
    void list_supportsPaginationAndSort() throws Exception {
        persistTransaction(owner, alimentacao, "A", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);
        persistTransaction(owner, alimentacao, "B", "20.00",
                LocalDate.of(2026, 5, 2), FormaPagamento.PIX);
        persistTransaction(owner, alimentacao, "C", "30.00",
                LocalDate.of(2026, 5, 3), FormaPagamento.PIX);

        mockMvc.perform(get("/transactions")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "data,asc")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].descricao").value("A"))
                .andExpect(jsonPath("$.content[1].descricao").value("B"));
    }

    @Test
    void list_defaultSortIsByDataDescending() throws Exception {
        persistTransaction(owner, alimentacao, "Antiga", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);
        persistTransaction(owner, alimentacao, "Recente", "20.00",
                LocalDate.of(2026, 5, 10), FormaPagamento.PIX);

        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descricao").value("Recente"))
                .andExpect(jsonPath("$.content[1].descricao").value("Antiga"));
    }

    @Test
    void update_modifiesTransactionAndReturnsResponse() throws Exception {
        Transaction tx = persistTransaction(owner, alimentacao, "Original", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);

        String payload = """
                {
                  "descricao": "Atualizado",
                  "valor": 75.00,
                  "data": "2026-05-20",
                  "formaPagamento": "CARTAO_CREDITO",
                  "categoryId": "%s"
                }
                """.formatted(lazer.getId());

        mockMvc.perform(put("/transactions/" + tx.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Atualizado"))
                .andExpect(jsonPath("$.valor").value(75.00))
                .andExpect(jsonPath("$.data").value("2026-05-20"))
                .andExpect(jsonPath("$.formaPagamento").value("CARTAO_CREDITO"))
                .andExpect(jsonPath("$.categoryId").value(lazer.getId().toString()));

        Transaction reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getDescricao()).isEqualTo("Atualizado");
        assertThat(reloaded.getCategory().getId()).isEqualTo(lazer.getId());
    }

    @Test
    void update_withNonExistentId_returns404() throws Exception {
        String payload = """
                {
                  "descricao": "x",
                  "valor": 1.00,
                  "data": "2026-05-01",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(put("/transactions/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:transaction-not-found"));
    }

    @Test
    void update_transactionFromAnotherUser_returns404() throws Exception {
        Transaction tx = persistTransaction(other, otherUserCategory, "Alheia", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);

        String payload = """
                {
                  "descricao": "Hack",
                  "valor": 1.00,
                  "data": "2026-05-01",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(put("/transactions/" + tx.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:transaction-not-found"));

        Transaction reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getDescricao()).isEqualTo("Alheia");
    }

    @Test
    void delete_removesTransactionAndReturns204() throws Exception {
        Transaction tx = persistTransaction(owner, alimentacao, "Apagar", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);

        mockMvc.perform(delete("/transactions/" + tx.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        assertThat(transactionRepository.findById(tx.getId())).isEmpty();
    }

    @Test
    void delete_transactionFromAnotherUser_returns404() throws Exception {
        Transaction tx = persistTransaction(other, otherUserCategory, "Alheia", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);

        mockMvc.perform(delete("/transactions/" + tx.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:transaction-not-found"));

        assertThat(transactionRepository.findById(tx.getId())).isPresent();
    }

    @Test
    void delete_withoutToken_returns401() throws Exception {
        Transaction tx = persistTransaction(owner, alimentacao, "x", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);
        mockMvc.perform(delete("/transactions/" + tx.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherUserSeesOnlyOwnTransactions() throws Exception {
        persistTransaction(owner, alimentacao, "Owner tx", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);
        persistTransaction(other, otherUserCategory, "Other tx", "20.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);

        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].descricao").value("Other tx"));
    }

    @Test
    void update_preservesInvoiceLink() throws Exception {
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .arquivo("fatura.pdf")
                .origem(OrigemFatura.OUTROS)
                .status(StatusFatura.PROCESSADA)
                .mesReferencia(YearMonth.of(2026, 5))
                .totalValor(new BigDecimal("123.45"))
                .user(owner)
                .build());

        Transaction tx = transactionRepository.save(Transaction.builder()
                .descricao("Da fatura")
                .valor(new BigDecimal("50.00"))
                .data(LocalDate.of(2026, 5, 1))
                .formaPagamento(FormaPagamento.CARTAO_CREDITO)
                .category(alimentacao)
                .invoice(invoice)
                .user(owner)
                .build());

        String payload = """
                {
                  "descricao": "Editada",
                  "valor": 60.00,
                  "data": "2026-05-15",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(lazer.getId());

        mockMvc.perform(put("/transactions/" + tx.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(invoice.getId().toString()));

        Transaction reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getInvoice()).isNotNull();
        assertThat(reloaded.getInvoice().getId()).isEqualTo(invoice.getId());
    }

    @Test
    void update_returnsFreshUpdatedAt() throws Exception {
        Transaction tx = persistTransaction(owner, alimentacao, "Original", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);
        OffsetDateTime initialUpdatedAt = tx.getUpdatedAt();
        // Garante uma diferenca de tempo perceptivel entre criacao e update.
        Thread.sleep(5);

        String payload = """
                {
                  "descricao": "Atualizado",
                  "valor": 20.00,
                  "data": "2026-05-02",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(alimentacao.getId());

        MvcResult result = mockMvc.perform(put("/transactions/" + tx.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        OffsetDateTime responseUpdatedAt = OffsetDateTime.parse(body.get("updatedAt").asText());

        assertThat(responseUpdatedAt).isAfterOrEqualTo(initialUpdatedAt);
        Transaction reloaded = transactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(reloaded.getUpdatedAt()).isEqualTo(responseUpdatedAt);
    }

    @Test
    void list_whenFromIsAfterTo_returns400() throws Exception {
        mockMvc.perform(get("/transactions")
                        .param("from", "2026-06-01")
                        .param("to", "2026-05-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:invalid-filter"));
    }

    @Test
    void list_whenRefCombinedWithRange_returns400() throws Exception {
        mockMvc.perform(get("/transactions")
                        .param("ref", "2026-05")
                        .param("from", "2026-05-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:invalid-filter"));
    }

    @Test
    void list_withInvalidTipo_returnsValidationProblemDetail() throws Exception {
        mockMvc.perform(get("/transactions")
                        .param("tipo", "FOO")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.tipo").exists());
    }

    @Test
    void list_withInvalidRef_returnsValidationProblemDetail() throws Exception {
        mockMvc.perform(get("/transactions")
                        .param("ref", "not-a-month")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.ref").exists());
    }

    @Test
    void list_ignoresUnsafeSortField() throws Exception {
        persistTransaction(owner, alimentacao, "Antiga", "10.00",
                LocalDate.of(2026, 5, 1), FormaPagamento.PIX);
        persistTransaction(owner, alimentacao, "Recente", "20.00",
                LocalDate.of(2026, 5, 10), FormaPagamento.PIX);

        // Sort em campo nao permitido: o servico cai para o default (data,desc) e nao 500.
        mockMvc.perform(get("/transactions")
                        .param("sort", "user.senha,asc")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descricao").value("Recente"))
                .andExpect(jsonPath("$.content[1].descricao").value("Antiga"));
    }

    @Test
    void list_capsPageSizeAtConfiguredMax() throws Exception {
        // max-page-size = 100 em application.yml
        mockMvc.perform(get("/transactions")
                        .param("size", "5000")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void create_withValorWithTooManyDecimals_returns400() throws Exception {
        String payload = """
                {
                  "descricao": "x",
                  "valor": 10.123,
                  "data": "2026-05-10",
                  "formaPagamento": "PIX",
                  "categoryId": "%s"
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.valor").exists());
    }

    private Transaction persistTransaction(User user, Category category, String descricao,
                                            String valor, LocalDate data, FormaPagamento forma) {
        return transactionRepository.save(Transaction.builder()
                .descricao(descricao)
                .valor(new BigDecimal(valor))
                .data(data)
                .formaPagamento(forma)
                .category(category)
                .user(user)
                .build());
    }
}
