package com.goodfunds.invoices;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.FormaPagamento;
import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.domain.StatusFatura;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.Transaction;
import com.goodfunds.domain.User;
import com.goodfunds.dto.InvoiceResponse;
import com.goodfunds.exception.InvoiceNotFoundException;
import com.goodfunds.invoice.parser.NubankInvoiceFixtures;
import com.goodfunds.repository.BudgetRepository;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.InvoiceRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
import com.goodfunds.service.InvoiceProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class InvoiceProcessingServiceIntegrationTest {

    @TempDir
    static Path uploadsDir;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.uploads.dir", () -> uploadsDir.toString());
    }

    @Autowired private InvoiceProcessingService processingService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private BudgetRepository budgetRepository;

    private User owner;
    private Category outros;

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
                .senha("hash")
                .build());
        outros = categoryRepository.save(Category.builder()
                .nome("Outros")
                .tipo(TipoCategoria.DESPESA)
                .user(owner)
                .build());
    }

    @Test
    void process_generatesTransactionsAndMarksProcessada() throws IOException {
        Invoice invoice = persistInvoiceWithSamplePdf();

        InvoiceResponse response = processingService.process(owner.getId(), invoice.getId());

        assertThat(response.status()).isEqualTo(StatusFatura.PROCESSADA);
        assertThat(response.mesReferencia()).isEqualTo(YearMonth.of(2025, 6));
        assertThat(response.totalValor()).isEqualByComparingTo("1234.56");

        Invoice persisted = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(StatusFatura.PROCESSADA);

        List<Transaction> transactions = transactionRepository.findByInvoiceId(invoice.getId());
        assertThat(transactions).hasSize(5);
        assertThat(transactions).allSatisfy(tx -> {
            assertThat(tx.getFormaPagamento()).isEqualTo(FormaPagamento.CARTAO_CREDITO);
            assertThat(tx.getCategory().getId()).isEqualTo(outros.getId());
            assertThat(tx.getUser().getId()).isEqualTo(owner.getId());
            assertThat(tx.getInvoice().getId()).isEqualTo(invoice.getId());
        });
        assertThat(transactions).extracting(Transaction::getDescricao)
                .containsExactlyInAnyOrder(
                        "MERCADO LIVRE", "UBER TRIP", "PADARIA CENTRAL", "NETFLIX", "POSTO SHELL");
    }

    @Test
    void process_calledTwice_isIdempotent() throws IOException {
        Invoice invoice = persistInvoiceWithSamplePdf();

        processingService.process(owner.getId(), invoice.getId());
        InvoiceResponse second = processingService.process(owner.getId(), invoice.getId());

        assertThat(second.status()).isEqualTo(StatusFatura.PROCESSADA);
        assertThat(transactionRepository.findByInvoiceId(invoice.getId())).hasSize(5);
    }

    @Test
    void process_forcedReprocess_replacesTransactionsWithoutDuplicating() throws IOException {
        Invoice invoice = persistInvoiceWithSamplePdf();

        processingService.process(owner.getId(), invoice.getId());

        // Simula um reprocessamento explicito (status reaberto) e garante que nao duplica.
        Invoice reopened = invoiceRepository.findById(invoice.getId()).orElseThrow();
        reopened.setStatus(StatusFatura.PENDENTE_PARSE);
        invoiceRepository.saveAndFlush(reopened);

        processingService.process(owner.getId(), invoice.getId());

        assertThat(transactionRepository.findByInvoiceId(invoice.getId())).hasSize(5);
    }

    @Test
    void process_whenPdfIsUnparseable_marksErroAndCreatesNoTransactions() throws IOException {
        Invoice invoice = persistInvoice("%PDF-1.4 conteudo invalido".getBytes());

        InvoiceResponse response = processingService.process(owner.getId(), invoice.getId());

        assertThat(response.status()).isEqualTo(StatusFatura.ERRO);
        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusFatura.ERRO);
        assertThat(transactionRepository.findByInvoiceId(invoice.getId())).isEmpty();
    }

    @Test
    void process_whenInvoiceMissing_throwsNotFound() {
        UUID missing = UUID.randomUUID();

        assertThatThrownBy(() -> processingService.process(owner.getId(), missing))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void process_whenInvoiceBelongsToAnotherUser_throwsNotFoundAndDoesNotProcess() throws IOException {
        Invoice invoice = persistInvoiceWithSamplePdf();
        User other = userRepository.save(User.builder()
                .nome("Other")
                .email("other@example.com")
                .senha("hash")
                .build());

        assertThatThrownBy(() -> processingService.process(other.getId(), invoice.getId()))
                .isInstanceOf(InvoiceNotFoundException.class);

        assertThat(transactionRepository.findByInvoiceId(invoice.getId())).isEmpty();
        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusFatura.PENDENTE_PARSE);
    }

    private Invoice persistInvoiceWithSamplePdf() throws IOException {
        String filename = UUID.randomUUID() + ".pdf";
        Path target = uploadsDir.resolve(owner.getId().toString()).resolve(filename);
        Files.createDirectories(target.getParent());
        NubankInvoiceFixtures.writeSamplePdf(target);
        return saveInvoice(owner.getId() + "/" + filename);
    }

    private Invoice persistInvoice(byte[] content) throws IOException {
        String filename = UUID.randomUUID() + ".pdf";
        Path target = uploadsDir.resolve(owner.getId().toString()).resolve(filename);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
        return saveInvoice(owner.getId() + "/" + filename);
    }

    private Invoice saveInvoice(String relativePath) {
        return invoiceRepository.save(Invoice.builder()
                .arquivo(relativePath)
                .origem(OrigemFatura.NUBANK)
                .status(StatusFatura.PENDENTE_PARSE)
                .user(owner)
                .build());
    }
}
