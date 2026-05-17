package com.goodfunds.invoices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.domain.StatusFatura;
import com.goodfunds.domain.User;
import com.goodfunds.repository.BudgetRepository;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.InvoiceRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
import com.goodfunds.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvoiceControllerIntegrationTest {

    @TempDir
    static Path uploadsDir;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.uploads.dir", () -> uploadsDir.toString());
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User owner;
    private String ownerToken;

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
        ownerToken = jwtService.generateToken(owner.getEmail());
    }

    @Test
    void upload_validPdf_returns201AndPersistsInvoice() throws Exception {
        MockMultipartFile file = pdfFile("fatura.pdf", "%PDF-1.4 conteudo".getBytes());

        MvcResult result = mockMvc.perform(multipart("/invoices/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/invoices/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.origem").value("NUBANK"))
                .andExpect(jsonPath("$.status").value("PENDENTE_PARSE"))
                .andExpect(jsonPath("$.arquivo").exists())
                .andExpect(jsonPath("$.mesReferencia").doesNotExist())
                .andExpect(jsonPath("$.totalValor").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID createdId = UUID.fromString(body.get("id").asText());

        Invoice persisted = invoiceRepository.findById(createdId).orElseThrow();
        assertThat(persisted.getUser().getId()).isEqualTo(owner.getId());
        assertThat(persisted.getOrigem()).isEqualTo(OrigemFatura.NUBANK);
        assertThat(persisted.getStatus()).isEqualTo(StatusFatura.PENDENTE_PARSE);
        assertThat(persisted.getMesReferencia()).isNull();
        assertThat(persisted.getTotalValor()).isNull();

        String relativePath = persisted.getArquivo();
        assertThat(relativePath).startsWith(owner.getId() + "/").endsWith(".pdf");
        Path savedFile = uploadsDir.resolve(relativePath);
        assertThat(Files.exists(savedFile)).isTrue();
        assertThat(Files.readAllBytes(savedFile)).isEqualTo("%PDF-1.4 conteudo".getBytes());
    }

    @Test
    void upload_withExplicitOrigem_persistsInformedOrigem() throws Exception {
        MockMultipartFile file = pdfFile("fatura.pdf", "%PDF-1.4 conteudo".getBytes());

        mockMvc.perform(multipart("/invoices/upload")
                        .file(file)
                        .param("origem", "ITAU")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origem").value("ITAU"));
    }

    @Test
    void upload_withoutToken_returns401() throws Exception {
        MockMultipartFile file = pdfFile("fatura.pdf", "%PDF-1.4 conteudo".getBytes());

        mockMvc.perform(multipart("/invoices/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_withoutFile_returns400() throws Exception {
        mockMvc.perform(multipart("/invoices/upload")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.file").exists());
    }

    @Test
    void upload_withNonPdfContentType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fatura.txt", "text/plain", "nao e pdf".getBytes());

        mockMvc.perform(multipart("/invoices/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:invalid-invoice-file"));
    }

    @Test
    void upload_withWrongMagicBytes_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fatura.pdf", MediaType.APPLICATION_PDF_VALUE, "NOTPDFBYTES".getBytes());

        mockMvc.perform(multipart("/invoices/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:invalid-invoice-file"));
    }

    @Test
    void upload_withInvalidOrigem_returns400() throws Exception {
        MockMultipartFile file = pdfFile("fatura.pdf", "%PDF-1.4 conteudo".getBytes());

        mockMvc.perform(multipart("/invoices/upload")
                        .file(file)
                        .param("origem", "INVALIDA")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:validation-error"));
    }

    @Test
    void upload_emptyFile_returns400() throws Exception {
        MockMultipartFile file = pdfFile("fatura.pdf", new byte[0]);

        mockMvc.perform(multipart("/invoices/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:invalid-invoice-file"));
    }

    private MockMultipartFile pdfFile(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, MediaType.APPLICATION_PDF_VALUE, content);
    }
}
