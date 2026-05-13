package com.goodfunds.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goodfunds.repository.CategoryRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanup() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_createsUserSeedsCategoriesAndReturnsToken() throws Exception {
        String payload = """
                {
                  "nome": "Fulano",
                  "email": "fulano@example.com",
                  "senha": "senha12345"
                }
                """;

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInMillis").value(86_400_000))
                .andReturn();

        assertThat(userRepository.existsByEmail("fulano@example.com")).isTrue();

        var user = userRepository.findByEmail("fulano@example.com").orElseThrow();
        assertThat(categoryRepository.findByUserId(user.getId())).hasSize(8);

        String token = readToken(result);
        assertThat(token).isNotBlank();
    }

    @Test
    void register_normalizesEmailToLowercase() throws Exception {
        String payload = """
                {
                  "nome": "Caps",
                  "email": "MIXED@Example.COM",
                  "senha": "senha12345"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        assertThat(userRepository.existsByEmail("mixed@example.com")).isTrue();
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        registerUser("dup@example.com", "senha12345");

        String payload = """
                {
                  "nome": "Outro",
                  "email": "dup@example.com",
                  "senha": "senha12345"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("E-mail ja cadastrado"));
    }

    @Test
    void register_withInvalidPayload_returns400() throws Exception {
        String payload = """
                {
                  "nome": "",
                  "email": "not-an-email",
                  "senha": "123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        registerUser("login@example.com", "senha12345");

        String payload = """
                {
                  "email": "login@example.com",
                  "senha": "senha12345"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        registerUser("wrongpass@example.com", "senha12345");

        String payload = """
                {
                  "email": "wrongpass@example.com",
                  "senha": "senha-errada"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Falha de autenticacao"));
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        String payload = """
                {
                  "email": "ghost@example.com",
                  "senha": "qualquercoisa"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_passesSecurity() throws Exception {
        String token = registerUser("authz@example.com", "senha12345");

        // /transactions ainda nao existe; o filtro deixa passar autenticado e o MVC responde 404.
        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/transactions").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpoints_areAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private String registerUser(String email, String senha) throws Exception {
        String payload = String.format("""
                {
                  "nome": "Test",
                  "email": "%s",
                  "senha": "%s"
                }
                """, email, senha);

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return readToken(result);
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }
}
