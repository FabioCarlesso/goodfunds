package com.goodfunds.service;

import com.goodfunds.config.InvoiceUploadProperties;
import com.goodfunds.domain.Category;
import com.goodfunds.domain.FormaPagamento;
import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.StatusFatura;
import com.goodfunds.domain.Transaction;
import com.goodfunds.domain.User;
import com.goodfunds.dto.InvoiceResponse;
import com.goodfunds.exception.InvoiceNotFoundException;
import com.goodfunds.invoice.parser.ParsedInvoice;
import com.goodfunds.invoice.parser.ParsedInvoiceTransaction;
import com.goodfunds.invoice.parser.InvoiceParserFactory;
import com.goodfunds.repository.CategoryRepository;
import com.goodfunds.repository.InvoiceRepository;
import com.goodfunds.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra o processamento de uma fatura ja enviada: extrai os lancamentos via
 * {@link InvoiceParserFactory}, gera as {@link Transaction}s correspondentes e atualiza
 * o estado da {@link Invoice}.
 */
@Service
public class InvoiceProcessingService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceProcessingService.class);

    /** Categoria padrao usada quando nao ha sugestao; semeada por usuario em {@code AuthService}. */
    private static final String DEFAULT_CATEGORY_NAME = "Outros";

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final InvoiceParserFactory parserFactory;
    private final InvoiceUploadProperties uploadProperties;

    public InvoiceProcessingService(InvoiceRepository invoiceRepository,
                                    TransactionRepository transactionRepository,
                                    CategoryRepository categoryRepository,
                                    InvoiceParserFactory parserFactory,
                                    InvoiceUploadProperties uploadProperties) {
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.parserFactory = parserFactory;
        this.uploadProperties = uploadProperties;
    }

    @Transactional
    public InvoiceResponse process(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (invoice.getStatus() == StatusFatura.PROCESSADA) {
            // Idempotencia: fatura ja processada nao gera novas transactions.
            log.debug("Fatura {} ja processada; reprocessamento ignorado", invoiceId);
            return InvoiceResponse.from(invoice);
        }

        try {
            ParsedInvoice parsed = parse(invoice);
            Category defaultCategory = resolveDefaultCategory(invoice.getUser());

            // Idempotencia: limpa lancamentos de uma tentativa anterior antes de recriar.
            transactionRepository.deleteByInvoiceId(invoiceId);

            List<Transaction> transactions = parsed.transacoes().stream()
                    .map(item -> toTransaction(item, invoice, defaultCategory))
                    .toList();
            transactionRepository.saveAll(transactions);

            invoice.setMesReferencia(parsed.mesReferencia());
            invoice.setTotalValor(parsed.total());
            invoice.setStatus(StatusFatura.PROCESSADA);
            log.info("Fatura {} processada: {} transactions geradas", invoiceId, transactions.size());
        } catch (RuntimeException ex) {
            // Falha de parse/persistencia marca a fatura como ERRO sem propagar o rollback.
            log.error("Falha ao processar fatura {}: {}", invoiceId, ex.getMessage(), ex);
            invoice.setStatus(StatusFatura.ERRO);
        }

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    private ParsedInvoice parse(Invoice invoice) {
        Path baseDir = Path.of(uploadProperties.getDir()).toAbsolutePath().normalize();
        File pdf = baseDir.resolve(invoice.getArquivo()).toFile();
        return parserFactory.forInvoice(invoice).parse(pdf);
    }

    private Category resolveDefaultCategory(User user) {
        return categoryRepository.findFirstByUserIdAndNomeIgnoreCase(user.getId(), DEFAULT_CATEGORY_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Categoria padrao '" + DEFAULT_CATEGORY_NAME
                                + "' nao encontrada para o usuario " + user.getId()));
    }

    private Transaction toTransaction(ParsedInvoiceTransaction item, Invoice invoice, Category category) {
        return Transaction.builder()
                .descricao(item.descricao())
                .valor(item.valor())
                .data(item.data())
                .formaPagamento(FormaPagamento.CARTAO_CREDITO)
                .category(category)
                .invoice(invoice)
                .user(invoice.getUser())
                .build();
    }
}
