package com.goodfunds.budgets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goodfunds.domain.Budget;
import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
class BudgetControllerIntegrationTest {

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

        alimentacao = persistCategory(owner, "Alimentacao");
        transporte = persistCategory(owner, "Transporte");
        alienCategory = persistCategory(other, "Alien");
    }

    @Test
    void create_persistsBudgetAndReturns201() throws Exception {
        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(alimentacao.getId());

        MvcResult result = mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/budgets/")))
                .andExpect(jsonPath("$.limite").value(500.00))
                .andExpect(jsonPath("$.categoryId").value(alimentacao.getId().toString()))
                .andExpect(jsonPath("$.categoryNome").value("Alimentacao"))
                .andExpect(jsonPath("$.mes").value(5))
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID createdId = UUID.fromString(body.get("id").asText());

        Budget persisted = budgetRepository.findById(createdId).orElseThrow();
        assertThat(persisted.getUser().getId()).isEqualTo(owner.getId());
        assertThat(persisted.getLimite()).isEqualByComparingTo("500.00");
    }

    @Test
    void create_duplicatePeriod_returns409() throws Exception {
        persistBudget(owner, alimentacao, new BigDecimal("300.00"), 5, 2026);

        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:budget-already-exists"))
                .andExpect(jsonPath("$.title").value("Orcamento ja existe"));

        assertThat(budgetRepository.findByUserIdAndAnoAndMes(owner.getId(), 2026, 5)).hasSize(1);
    }

    @Test
    void create_withCategoryFromAnotherUser_returns404() throws Exception {
        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(alienCategory.getId());

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-not-found"));
    }

    @Test
    void create_withNonPositiveLimite_returns400() throws Exception {
        String payload = """
                {
                  "limite": -10.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.limite").exists());
    }

    @Test
    void create_withMesOutOfRange_returns400() throws Exception {
        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 13,
                  "ano": 2026
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.mes").exists());
    }

    @Test
    void create_withAnoOutOfRange_returns400() throws Exception {
        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 999999
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.ano").exists());
    }

    @Test
    void create_withNullCategoryId_returns400() throws Exception {
        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": null,
                  "mes": 5,
                  "ano": 2026
                }
                """;

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(post("/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsOnlyOwnerBudgetsForMonthOrderedByCategoryName() throws Exception {
        persistBudget(owner, transporte, new BigDecimal("200.00"), 5, 2026);
        persistBudget(owner, alimentacao, new BigDecimal("500.00"), 5, 2026);
        persistBudget(owner, alimentacao, new BigDecimal("450.00"), 4, 2026);
        persistBudget(other, alienCategory, new BigDecimal("900.00"), 5, 2026);

        mockMvc.perform(get("/budgets")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryNome").value("Alimentacao"))
                .andExpect(jsonPath("$[1].categoryNome").value("Transporte"));
    }

    @Test
    void list_withoutRef_returns400() throws Exception {
        mockMvc.perform(get("/budgets")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.ref").exists());
    }

    @Test
    void list_withMalformedRef_returns400() throws Exception {
        mockMvc.perform(get("/budgets")
                        .param("ref", "2026-13")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.errors.ref").exists());
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/budgets").param("ref", "2026-05"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_modifiesBudgetAndReturns200() throws Exception {
        Budget budget = persistBudget(owner, alimentacao, new BigDecimal("300.00"), 5, 2026);

        String payload = """
                {
                  "limite": 800.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(put("/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limite").value(800.00));

        Budget reloaded = budgetRepository.findById(budget.getId()).orElseThrow();
        assertThat(reloaded.getLimite()).isEqualByComparingTo("800.00");
    }

    @Test
    void update_withNonExistentId_returns404() throws Exception {
        String payload = """
                {
                  "limite": 800.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(put("/budgets/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:budget-not-found"));
    }

    @Test
    void update_budgetFromAnotherUser_returns404() throws Exception {
        Budget alien = persistBudget(other, alienCategory, new BigDecimal("100.00"), 5, 2026);

        String payload = """
                {
                  "limite": 800.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(put("/budgets/" + alien.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:budget-not-found"));

        Budget reloaded = budgetRepository.findById(alien.getId()).orElseThrow();
        assertThat(reloaded.getLimite()).isEqualByComparingTo("100.00");
    }

    @Test
    void update_ontoExistingPeriod_returns409() throws Exception {
        Budget toMove = persistBudget(owner, alimentacao, new BigDecimal("300.00"), 5, 2026);
        persistBudget(owner, alimentacao, new BigDecimal("700.00"), 6, 2026);

        String payload = """
                {
                  "limite": 400.00,
                  "categoryId": "%s",
                  "mes": 6,
                  "ano": 2026
                }
                """.formatted(alimentacao.getId());

        mockMvc.perform(put("/budgets/" + toMove.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:budget-already-exists"));

        Budget reloaded = budgetRepository.findById(toMove.getId()).orElseThrow();
        assertThat(reloaded.getMes()).isEqualTo(5);
    }

    @Test
    void update_withoutToken_returns401() throws Exception {
        Budget budget = persistBudget(owner, alimentacao, new BigDecimal("300.00"), 5, 2026);

        mockMvc.perform(put("/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private Category persistCategory(User user, String nome) {
        return categoryRepository.save(Category.builder()
                .nome(nome)
                .tipo(TipoCategoria.DESPESA)
                .user(user)
                .build());
    }

    private Budget persistBudget(User user, Category category, BigDecimal limite, int mes, int ano) {
        return budgetRepository.save(Budget.builder()
                .limite(limite)
                .category(category)
                .mes(mes)
                .ano(ano)
                .user(user)
                .build());
    }
}
