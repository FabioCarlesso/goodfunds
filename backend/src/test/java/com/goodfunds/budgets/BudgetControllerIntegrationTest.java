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
    private String otherToken;
    private Category ownerCategory;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        invoiceRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .nome("Owner")
                .email("owner@budget.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());
        other = userRepository.save(User.builder()
                .nome("Other")
                .email("other@budget.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());

        ownerToken = jwtService.generateToken(owner.getEmail());
        otherToken = jwtService.generateToken(other.getEmail());

        ownerCategory = categoryRepository.save(Category.builder()
                .nome("Alimentacao")
                .tipo(TipoCategoria.DESPESA)
                .user(owner)
                .build());
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
                """.formatted(ownerCategory.getId());

        MvcResult result = mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/budgets/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.limite").value(500.00))
                .andExpect(jsonPath("$.mes").value(5))
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.category.id").value(ownerCategory.getId().toString()))
                .andExpect(jsonPath("$.category.nome").value("Alimentacao"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID createdId = UUID.fromString(body.get("id").asText());
        assertThat(budgetRepository.findById(createdId)).isPresent();
    }

    @Test
    void create_duplicateCombo_returns409() throws Exception {
        budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("300.00"))
                .category(ownerCategory)
                .mes(5)
                .ano(2026)
                .user(owner)
                .build());

        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(ownerCategory.getId());

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:budget-duplicate"))
                .andExpect(jsonPath("$.title").value("Orcamento duplicado"));
    }

    @Test
    void create_withCategoryFromAnotherUser_returns404() throws Exception {
        Category otherCategory = categoryRepository.save(Category.builder()
                .nome("Lazer")
                .tipo(TipoCategoria.DESPESA)
                .user(other)
                .build());

        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(otherCategory.getId());

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-not-found"));
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
                """.formatted(ownerCategory.getId());

        mockMvc.perform(post("/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withInvalidPayload_returns400() throws Exception {
        String payload = """
                {
                  "limite": -10,
                  "categoryId": "%s",
                  "mes": 13,
                  "ano": 1999
                }
                """.formatted(ownerCategory.getId());

        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"));
    }

    @Test
    void list_withRef_returnsFilteredBudgets() throws Exception {
        budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("500.00"))
                .category(ownerCategory)
                .mes(5)
                .ano(2026)
                .user(owner)
                .build());
        budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("300.00"))
                .category(ownerCategory)
                .mes(4)
                .ano(2026)
                .user(owner)
                .build());

        mockMvc.perform(get("/budgets")
                        .param("ref", "2026-05")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].mes").value(5));
    }

    @Test
    void list_withoutRef_returnsAllOwnerBudgets() throws Exception {
        budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("500.00"))
                .category(ownerCategory)
                .mes(5)
                .ano(2026)
                .user(owner)
                .build());
        budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("300.00"))
                .category(ownerCategory)
                .mes(4)
                .ano(2026)
                .user(owner)
                .build());

        Category otherCategory = categoryRepository.save(Category.builder()
                .nome("Outro")
                .tipo(TipoCategoria.DESPESA)
                .user(other)
                .build());
        budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("100.00"))
                .category(otherCategory)
                .mes(5)
                .ano(2026)
                .user(other)
                .build());

        mockMvc.perform(get("/budgets")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/budgets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_modifiesBudgetAndReturns200() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("300.00"))
                .category(ownerCategory)
                .mes(5)
                .ano(2026)
                .user(owner)
                .build());

        String payload = """
                {
                  "limite": 800.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(ownerCategory.getId());

        mockMvc.perform(put("/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limite").value(800.00))
                .andExpect(jsonPath("$.mes").value(5))
                .andExpect(jsonPath("$.ano").value(2026));

        Budget reloaded = budgetRepository.findById(budget.getId()).orElseThrow();
        assertThat(reloaded.getLimite()).isEqualByComparingTo("800.00");
    }

    @Test
    void update_withNonExistentId_returns404() throws Exception {
        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(ownerCategory.getId());

        mockMvc.perform(put("/budgets/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:budget-not-found"));
    }

    @Test
    void update_budgetFromAnotherUser_returns404() throws Exception {
        Category otherCategory = categoryRepository.save(Category.builder()
                .nome("Lazer")
                .tipo(TipoCategoria.DESPESA)
                .user(other)
                .build());
        Budget otherBudget = budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("300.00"))
                .category(otherCategory)
                .mes(5)
                .ano(2026)
                .user(other)
                .build());

        String payload = """
                {
                  "limite": 999.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(ownerCategory.getId());

        mockMvc.perform(put("/budgets/" + otherBudget.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:budget-not-found"));
    }

    @Test
    void update_toConflictingCombo_returns409() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("300.00"))
                .category(ownerCategory)
                .mes(5)
                .ano(2026)
                .user(owner)
                .build());

        Category ownerCategory2 = categoryRepository.save(Category.builder()
                .nome("Transporte")
                .tipo(TipoCategoria.DESPESA)
                .user(owner)
                .build());
        budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("200.00"))
                .category(ownerCategory2)
                .mes(6)
                .ano(2026)
                .user(owner)
                .build());

        String payload = """
                {
                  "limite": 500.00,
                  "categoryId": "%s",
                  "mes": 6,
                  "ano": 2026
                }
                """.formatted(ownerCategory2.getId());

        mockMvc.perform(put("/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:budget-duplicate"));
    }

    @Test
    void update_withSameComboAsItself_succeeds() throws Exception {
        Budget budget = budgetRepository.save(Budget.builder()
                .limite(new BigDecimal("300.00"))
                .category(ownerCategory)
                .mes(5)
                .ano(2026)
                .user(owner)
                .build());

        String payload = """
                {
                  "limite": 700.00,
                  "categoryId": "%s",
                  "mes": 5,
                  "ano": 2026
                }
                """.formatted(ownerCategory.getId());

        mockMvc.perform(put("/budgets/" + budget.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limite").value(700.00));
    }
}
