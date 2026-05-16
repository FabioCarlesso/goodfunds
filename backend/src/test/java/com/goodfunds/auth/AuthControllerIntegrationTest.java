package com.goodfunds.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goodfunds.domain.User;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        transactionRepository.deleteAll();
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:email-already-in-use"))
                .andExpect(jsonPath("$.title").value("E-mail ja cadastrado"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.instance").value("/auth/register"));
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"))
                .andExpect(jsonPath("$.title").value("Erro de validacao"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/auth/register"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void register_withMalformedJson_returnsProblemDetail400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:invalid-request-body"))
                .andExpect(jsonPath("$.title").value("Corpo da requisicao invalido"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/auth/register"));
    }

    @Test
    void register_withUnsupportedContentType_returnsProblemDetail415() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:unsupported-media-type"))
                .andExpect(jsonPath("$.title").value("Tipo de conteudo nao suportado"))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.instance").value("/auth/register"));
    }

    @Test
    void login_withUnsupportedMethod_returnsProblemDetail405() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:method-not-allowed"))
                .andExpect(jsonPath("$.title").value("Metodo nao suportado"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.instance").value("/auth/login"));
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:authentication-failed"))
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
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:unauthenticated"))
                .andExpect(jsonPath("$.title").value("Nao autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/transactions"));
    }

    @Test
    void protectedEndpoint_withValidToken_passesSecurity() throws Exception {
        String token = registerUser("authz@example.com", "senha12345");

        // Com token valido, /transactions responde 200 com uma Page vazia.
        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void protectedEndpoint_withValidTokenOnUnknownPath_returnsProblemDetail404() throws Exception {
        String token = registerUser("authz-404@example.com", "senha12345");

        mockMvc.perform(get("/rota-inexistente").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:resource-not-found"))
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/rota-inexistente"));
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/transactions").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:unauthenticated"));
    }

    @Test
    void login_withDisabledUser_returns403() throws Exception {
        User disabledUser = User.builder()
                .nome("Disabled")
                .email("disabled@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .enabled(false)
                .build();
        userRepository.save(disabledUser);

        String payload = """
                {
                  "email": "disabled@example.com",
                  "senha": "senha12345"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:user-disabled"))
                .andExpect(jsonPath("$.title").value("Usuario desabilitado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.instance").value("/auth/login"));
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
