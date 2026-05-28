package com.goodfunds.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goodfunds.repository.BudgetRepository;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre os cenarios de hardening do criterio #31:
 * - 401 quando rota autenticada e chamada sem token.
 * - Isolamento entre usuarios: usuario A nao consegue ler/editar/excluir recursos de B.
 *
 * Decisao de design: para recursos de outro usuario respondemos 404 (e nao 403) para nao
 * vazar a existencia do id; os services usam {@code findByIdAndUserId} e disparam
 * {@code ResourceNotFoundException} quando o id nao pertence ao usuario logado. O 403
 * fica reservado para {@link org.springframework.security.access.AccessDeniedException}
 * (regras de autorizacao em endpoint), tratado pelo
 * {@link ProblemDetailAccessDeniedHandler}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationIsolationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private BudgetRepository budgetRepository;

    @BeforeEach
    void cleanup() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void protectedRoutes_withoutToken_return401() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:unauthenticated"));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/budgets?ref=2026-01"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/reports/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void categoryOfAnotherUser_isNotVisible() throws Exception {
        String tokenA = registerUser("a@example.com");
        String tokenB = registerUser("b@example.com");

        String categoryId = createCategory(tokenA, "Categoria de A");

        // GET nao expoe a categoria de A na listagem de B
        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + categoryId + "')]").isEmpty());

        // PUT em categoria de A com token de B retorna 404 (sem vazar existencia)
        String updatePayload = """
                {"nome": "alterada", "tipo": "DESPESA"}
                """;
        mockMvc.perform(put("/categories/" + categoryId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-not-found"));

        // DELETE em categoria de A com token de B tambem 404
        mockMvc.perform(delete("/categories/" + categoryId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:category-not-found"));
    }

    @Test
    void transactionOfAnotherUser_isNotAccessible() throws Exception {
        String tokenA = registerUser("a-tx@example.com");
        String tokenB = registerUser("b-tx@example.com");

        String categoryIdA = createCategory(tokenA, "Categoria Tx A");
        String transactionId = createTransaction(tokenA, categoryIdA);
        String categoryIdB = createCategory(tokenB, "Categoria Tx B");

        // PUT em transacao de A com token de B retorna 404
        String updatePayload = String.format("""
                {"descricao": "hack", "valor": 99.00, "data": "2026-01-15",
                 "formaPagamento": "PIX", "categoryId": "%s"}
                """, categoryIdB);
        mockMvc.perform(put("/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:transaction-not-found"));

        // DELETE em transacao de A com token de B tambem 404
        mockMvc.perform(delete("/transactions/" + transactionId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:transaction-not-found"));
    }

    @Test
    void budgetOfAnotherUser_isNotAccessible() throws Exception {
        String tokenA = registerUser("a-bg@example.com");
        String tokenB = registerUser("b-bg@example.com");

        String categoryIdA = createCategory(tokenA, "Categoria Budget A");
        String budgetId = createBudget(tokenA, categoryIdA);
        String categoryIdB = createCategory(tokenB, "Categoria Budget B");

        // PUT em orcamento de A com token de B retorna 404
        String updatePayload = String.format("""
                {"limite": 999.00, "categoryId": "%s", "mes": 5, "ano": 2026}
                """, categoryIdB);
        mockMvc.perform(put("/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:budget-not-found"));
    }

    private String registerUser(String email) throws Exception {
        String payload = String.format("""
                {"nome": "User", "email": "%s", "senha": "senha12345"}
                """, email);
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    private String createCategory(String token, String nome) throws Exception {
        String payload = String.format("""
                {"nome": "%s", "tipo": "DESPESA"}
                """, nome);
        MvcResult result = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    private String createTransaction(String token, String categoryId) throws Exception {
        String payload = String.format("""
                {"descricao": "compra", "valor": 50.00, "data": "2026-01-10",
                 "formaPagamento": "PIX", "categoryId": "%s"}
                """, categoryId);
        MvcResult result = mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    private String createBudget(String token, String categoryId) throws Exception {
        String payload = String.format("""
                {"limite": 500.00, "categoryId": "%s", "mes": 5, "ano": 2026}
                """, categoryId);
        MvcResult result = mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }
}
