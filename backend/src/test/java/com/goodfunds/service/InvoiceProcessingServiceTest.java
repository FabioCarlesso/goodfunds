package com.goodfunds.service;

import com.goodfunds.config.InvoiceUploadProperties;
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
import com.goodfunds.invoice.parser.InvoiceParseException;
import com.goodfunds.invoice.parser.InvoiceParser;
import com.goodfunds.invoice.parser.InvoiceParserFactory;
import com.goodfunds.invoice.parser.ParsedInvoice;
import com.goodfunds.invoice.parser.ParsedInvoiceTransaction;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.InvoiceRepository;
import com.goodfunds.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceProcessingServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private InvoiceParserFactory parserFactory;
    @Mock private InvoiceParser parser;

    private InvoiceProcessingService service;

    private User owner;
    private Category outros;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        InvoiceUploadProperties properties = new InvoiceUploadProperties();
        service = new InvoiceProcessingService(
                invoiceRepository, transactionRepository, categoryRepository, parserFactory, properties);

        owner = User.builder()
                .id(UUID.randomUUID())
                .nome("Owner")
                .email("owner@example.com")
                .senha("hash")
                .build();
        outros = Category.builder()
                .id(UUID.randomUUID())
                .nome("Outros")
                .tipo(TipoCategoria.DESPESA)
                .user(owner)
                .build();
        invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .arquivo(owner.getId() + "/fatura.pdf")
                .origem(OrigemFatura.NUBANK)
                .status(StatusFatura.PENDENTE_PARSE)
                .user(owner)
                .build();
    }

    @Test
    void process_parsesAndGeneratesTransactions_marksProcessada() {
        ParsedInvoice parsed = new ParsedInvoice(
                YearMonth.of(2025, 6),
                new BigDecimal("125.30"),
                List.of(
                        new ParsedInvoiceTransaction(LocalDate.of(2025, 6, 1), "MERCADO", new BigDecimal("89.90")),
                        new ParsedInvoiceTransaction(LocalDate.of(2025, 6, 5), "UBER", new BigDecimal("35.40"))));
        stubParsing(parsed);
        when(categoryRepository.findFirstByUserIdAndNomeIgnoreCaseOrderByIdAsc(owner.getId(), "Outros"))
                .thenReturn(Optional.of(outros));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));

        InvoiceResponse response = service.process(owner.getId(), invoice.getId());

        assertThat(response.status()).isEqualTo(StatusFatura.PROCESSADA);
        assertThat(response.mesReferencia()).isEqualTo(YearMonth.of(2025, 6));
        assertThat(response.totalValor()).isEqualByComparingTo("125.30");
        assertThat(invoice.getStatus()).isEqualTo(StatusFatura.PROCESSADA);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        List<Transaction> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(tx -> {
            assertThat(tx.getFormaPagamento()).isEqualTo(FormaPagamento.CARTAO_CREDITO);
            assertThat(tx.getCategory()).isEqualTo(outros);
            assertThat(tx.getInvoice()).isEqualTo(invoice);
            assertThat(tx.getUser()).isEqualTo(owner);
        });
        assertThat(saved).extracting(Transaction::getDescricao).containsExactly("MERCADO", "UBER");
    }

    @Test
    void process_clearsPreviousTransactionsBeforeRecreating() {
        ParsedInvoice parsed = new ParsedInvoice(
                YearMonth.of(2025, 6),
                new BigDecimal("89.90"),
                List.of(new ParsedInvoiceTransaction(LocalDate.of(2025, 6, 1), "MERCADO", new BigDecimal("89.90"))));
        stubParsing(parsed);
        when(categoryRepository.findFirstByUserIdAndNomeIgnoreCaseOrderByIdAsc(owner.getId(), "Outros"))
                .thenReturn(Optional.of(outros));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));

        service.process(owner.getId(), invoice.getId());

        verify(transactionRepository).deleteByInvoiceId(invoice.getId());
    }

    @Test
    void process_whenAlreadyProcessada_returnsWithoutReprocessing() {
        invoice.setStatus(StatusFatura.PROCESSADA);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceResponse response = service.process(owner.getId(), invoice.getId());

        assertThat(response.status()).isEqualTo(StatusFatura.PROCESSADA);
        verify(parserFactory, never()).forInvoice(any());
        verify(transactionRepository, never()).deleteByInvoiceId(any());
        verify(transactionRepository, never()).saveAll(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void process_whenParseFails_marksErro() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(parserFactory.forInvoice(invoice)).thenReturn(parser);
        when(parser.parse(any(File.class))).thenThrow(new InvoiceParseException("fatura ilegivel"));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));

        InvoiceResponse response = service.process(owner.getId(), invoice.getId());

        assertThat(response.status()).isEqualTo(StatusFatura.ERRO);
        assertThat(invoice.getStatus()).isEqualTo(StatusFatura.ERRO);
        verify(transactionRepository, never()).deleteByInvoiceId(any());
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void process_whenDefaultCategoryMissing_marksErro() {
        ParsedInvoice parsed = new ParsedInvoice(
                YearMonth.of(2025, 6),
                new BigDecimal("89.90"),
                List.of(new ParsedInvoiceTransaction(LocalDate.of(2025, 6, 1), "MERCADO", new BigDecimal("89.90"))));
        stubParsing(parsed);
        when(categoryRepository.findFirstByUserIdAndNomeIgnoreCaseOrderByIdAsc(owner.getId(), "Outros"))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));

        InvoiceResponse response = service.process(owner.getId(), invoice.getId());

        assertThat(response.status()).isEqualTo(StatusFatura.ERRO);
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void process_whenInvoiceNotFound_throws() {
        UUID missing = UUID.randomUUID();
        when(invoiceRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.process(owner.getId(), missing))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void process_whenInvoiceBelongsToAnotherUser_throwsNotFound() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        UUID anotherUser = UUID.randomUUID();

        assertThatThrownBy(() -> service.process(anotherUser, invoice.getId()))
                .isInstanceOf(InvoiceNotFoundException.class);

        verify(parserFactory, never()).forInvoice(any());
        verify(transactionRepository, never()).deleteByInvoiceId(any());
        verify(transactionRepository, never()).saveAll(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void process_whenLineItemValueNotPositive_marksErroWithoutPersisting() {
        ParsedInvoice parsed = new ParsedInvoice(
                YearMonth.of(2025, 6),
                new BigDecimal("89.90"),
                List.of(new ParsedInvoiceTransaction(LocalDate.of(2025, 6, 1), "ESTORNO", new BigDecimal("-50.00"))));
        stubParsing(parsed);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));

        InvoiceResponse response = service.process(owner.getId(), invoice.getId());

        assertThat(response.status()).isEqualTo(StatusFatura.ERRO);
        assertThat(invoice.getStatus()).isEqualTo(StatusFatura.ERRO);
        verify(transactionRepository, never()).deleteByInvoiceId(any());
        verify(transactionRepository, never()).saveAll(any());
    }

    private void stubParsing(ParsedInvoice parsed) {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(parserFactory.forInvoice(invoice)).thenReturn(parser);
        lenient().when(parser.parse(any(File.class))).thenReturn(parsed);
    }
}
