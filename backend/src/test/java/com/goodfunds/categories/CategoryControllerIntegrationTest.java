package com.goodfunds.categories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerIntegrationTest {

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

        ownerToken = jwtService.generateToken(owner.getEmail());
        otherToken = jwtService.generateToken(other.getEmail());
    }

    @Test
    void create_persistsCategoryAndReturns201() throws Exception {
        String payload = """
                {
                  "nome": "Investimentos",
                  "tipo": "DESPESA"
                }
                """;

        MvcResult result = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/categories/")))
                .andExpect(jsonPath("$.nome").value("Investimentos"))
                .andExpect(jsonPath("$.tipo").value("DESPESA"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID createdId = UUID.fromString(body.get("id").asText());

        Category persisted = categoryRepository.findById(createdId).orElseThrow();
        assertThat(persisted.getUser().getId()).isEqualTo(owner.getId());
        assertThat(persisted.getNome()).isEqualTo("Investimentos");
    }

    @Test
    void create_trimsNome() throws Exception {
        String payload = """
                {
                  "nome": "  Lazer  ",
                  "tipo": "DESPESA"
                }
                """;

        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Lazer"));
    }

    @Test
    void create_withBlankNome_returns400() throws Exception {
        String payload = """
                {
                  "nome": "",
                  "tipo": "DESPESA"
                }
                """;

        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.nome").exists());
    }

    @Test
    void create_withMissingTipo_returns400() throws Exception {
        String payload = """
                {
                  "nome": "Investimentos",
                  "tipo": null
                }
                """;

        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.tipo").exists());
    }

    @Test
    void create_withInvalidTipo_returns400() throws Exception {
        String payload = """
                {
                  "nome": "Investimentos",
                  "tipo": "FOO"
                }
                """;

        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        String payload = """
                {
                  "nome": "X",
                  "tipo": "DESPESA"
                }
                """;

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsOnlyOwnerCategoriesOrderedByName() throws Exception {
        persistCategory(owner, "Zebra", TipoCategoria.DESPESA);
        persistCategory(owner, "Alimentacao", TipoCategoria.DESPESA);
        persistCategory(owner, "Salario", TipoCategoria.RECEITA);
        persistCategory(other, "Nao deve aparecer", TipoCategoria.DESPESA);

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nome").value("Alimentacao"))
                .andExpect(jsonPath("$[1].nome").value("Salario"))
                .andExpect(jsonPath("$[2].nome").value("Zebra"));
    }

    @Test
    void list_filterByTipo_returnsOnlyMatching() throws Exception {
        persistCategory(owner, "Alimentacao", TipoCategoria.DESPESA);
        persistCategory(owner, "Salario", TipoCategoria.RECEITA);

        mockMvc.perform(get("/categories")
                        .param("tipo", "RECEITA")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Salario"));
    }

    @Test
    void list_withInvalidTipo_returns400() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("tipo", "INVALIDO")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"));
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_modifiesCategoryAndReturnsResponse() throws Exception {
        Category category = persistCategory(owner, "Original", TipoCategoria.DESPESA);

        String payload = """
                {
                  "nome": "Atualizada",
                  "tipo": "RECEITA"
                }
                """;

        mockMvc.perform(put("/categories/" + category.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Atualizada"))
                .andExpect(jsonPath("$.tipo").value("RECEITA"));

        Category reloaded = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(reloaded.getNome()).isEqualTo("Atualizada");
        assertThat(reloaded.getTipo()).isEqualTo(TipoCategoria.RECEITA);
    }

    @Test
    void update_withNonExistentId_returns404() throws Exception {
        String payload = """
                {
                  "nome": "X",
                  "tipo": "DESPESA"
                }
                """;

        mockMvc.perform(put("/categories/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-not-found"));
    }

    @Test
    void update_categoryFromAnotherUser_returns404() throws Exception {
        Category alien = persistCategory(other, "Alien", TipoCategoria.DESPESA);

        String payload = """
                {
                  "nome": "Hack",
                  "tipo": "RECEITA"
                }
                """;

        mockMvc.perform(put("/categories/" + alien.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-not-found"));

        Category reloaded = categoryRepository.findById(alien.getId()).orElseThrow();
        assertThat(reloaded.getNome()).isEqualTo("Alien");
    }

    @Test
    void delete_removesCategoryAndReturns204() throws Exception {
        Category category = persistCategory(owner, "Lazer", TipoCategoria.DESPESA);

        mockMvc.perform(delete("/categories/" + category.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        assertThat(categoryRepository.findById(category.getId())).isEmpty();
    }

    @Test
    void delete_categoryFromAnotherUser_returns404() throws Exception {
        Category alien = persistCategory(other, "Alien", TipoCategoria.DESPESA);

        mockMvc.perform(delete("/categories/" + alien.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-not-found"));

        assertThat(categoryRepository.findById(alien.getId())).isPresent();
    }

    @Test
    void delete_whenInUseByTransaction_returns409() throws Exception {
        Category category = persistCategory(owner, "Lazer", TipoCategoria.DESPESA);
        transactionRepository.save(Transaction.builder()
                .descricao("Cinema")
                .valor(new BigDecimal("30.00"))
                .data(LocalDate.of(2026, 5, 1))
                .formaPagamento(FormaPagamento.DINHEIRO)
                .category(category)
                .user(owner)
                .build());

        mockMvc.perform(delete("/categories/" + category.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-in-use"))
                .andExpect(jsonPath("$.title").value("Categoria em uso"));

        assertThat(categoryRepository.findById(category.getId())).isPresent();
    }

    @Test
    void delete_whenInUseByBudget_returns409() throws Exception {
        Category category = persistCategory(owner, "Alimentacao", TipoCategoria.DESPESA);
        budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("500.00"))
                .ano(2026)
                .mes(5)
                .category(category)
                .user(owner)
                .build());

        mockMvc.perform(delete("/categories/" + category.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-in-use"));

        assertThat(categoryRepository.findById(category.getId())).isPresent();
    }

    @Test
    void delete_withoutToken_returns401() throws Exception {
        Category category = persistCategory(owner, "Lazer", TipoCategoria.DESPESA);
        mockMvc.perform(delete("/categories/" + category.getId()))
                .andExpect(status().isUnauthorized());
    }

    private Category persistCategory(User user, String nome, TipoCategoria tipo) {
        return categoryRepository.save(Category.builder()
                .nome(nome)
                .tipo(tipo)
                .user(user)
                .build());
    }
}
