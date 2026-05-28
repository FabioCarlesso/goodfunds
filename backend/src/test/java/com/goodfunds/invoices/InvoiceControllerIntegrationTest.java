package com.goodfunds.invoices;

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

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void list_returnsOnlyOwnerInvoicesOrderedByCreatedAtDesc() throws Exception {
        Invoice older = invoiceRepository.save(Invoice.builder()
                .arquivo(owner.getId() + "/older.pdf")
                .origem(OrigemFatura.NUBANK)
                .status(StatusFatura.PROCESSADA)
                .mesReferencia(YearMonth.of(2026, 4))
                .totalValor(new BigDecimal("100.00"))
                .user(owner)
                .build());
        Invoice newer = invoiceRepository.save(Invoice.builder()
                .arquivo(owner.getId() + "/newer.pdf")
                .origem(OrigemFatura.ITAU)
                .status(StatusFatura.PENDENTE_PARSE)
                .user(owner)
                .build());

        User other = userRepository.save(User.builder()
                .nome("Other")
                .email("other@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());
        invoiceRepository.save(Invoice.builder()
                .arquivo(other.getId() + "/alheia.pdf")
                .origem(OrigemFatura.NUBANK)
                .status(StatusFatura.PROCESSADA)
                .user(other)
                .build());

        mockMvc.perform(get("/invoices")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newer.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(older.getId().toString()))
                .andExpect(jsonPath("$[1].totalValor").value(100.00));
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/invoices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_returnsInvoiceWithTransactionsOrderedByDataAsc() throws Exception {
        Category alimentacao = categoryRepository.save(Category.builder()
                .nome("Alimentacao")
                .tipo(TipoCategoria.DESPESA)
                .user(owner)
                .build());

        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .arquivo(owner.getId() + "/fatura.pdf")
                .origem(OrigemFatura.NUBANK)
                .status(StatusFatura.PROCESSADA)
                .mesReferencia(YearMonth.of(2026, 5))
                .totalValor(new BigDecimal("75.50"))
                .user(owner)
                .build());

        Transaction later = transactionRepository.save(Transaction.builder()
                .descricao("Almoco")
                .valor(new BigDecimal("50.00"))
                .data(LocalDate.of(2026, 5, 20))
                .formaPagamento(FormaPagamento.CARTAO_CREDITO)
                .category(alimentacao)
                .invoice(invoice)
                .user(owner)
                .build());
        Transaction earlier = transactionRepository.save(Transaction.builder()
                .descricao("Cafe")
                .valor(new BigDecimal("25.50"))
                .data(LocalDate.of(2026, 5, 10))
                .formaPagamento(FormaPagamento.CARTAO_CREDITO)
                .category(alimentacao)
                .invoice(invoice)
                .user(owner)
                .build());

        mockMvc.perform(get("/invoices/{id}", invoice.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoice.getId().toString()))
                .andExpect(jsonPath("$.mesReferencia").value("2026-05"))
                .andExpect(jsonPath("$.totalValor").value(75.50))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[0].id").value(earlier.getId().toString()))
                .andExpect(jsonPath("$.transactions[0].categoryNome").value("Alimentacao"))
                .andExpect(jsonPath("$.transactions[1].id").value(later.getId().toString()));
    }

    @Test
    void get_invoiceFromAnotherUser_returns404() throws Exception {
        User other = userRepository.save(User.builder()
                .nome("Other")
                .email("other@example.com")
                .senha(passwordEncoder.encode("senha12345"))
                .build());
        Invoice alheia = invoiceRepository.save(Invoice.builder()
                .arquivo(other.getId() + "/alheia.pdf")
                .origem(OrigemFatura.NUBANK)
                .status(StatusFatura.PROCESSADA)
                .user(other)
                .build());

        mockMvc.perform(get("/invoices/{id}", alheia.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:invoice-not-found"));
    }

    @Test
    void get_unknownInvoice_returns404() throws Exception {
        mockMvc.perform(get("/invoices/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:goodfunds:problem:invoice-not-found"));
    }
}
