package com.goodfunds.service;

import com.goodfunds.config.InvoiceUploadProperties;
import com.goodfunds.domain.Category;
import com.goodfunds.domain.FormaPagamento;
import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.StatusFatura;
import com.goodfunds.domain.Transaction;
import com.goodfunds.dto.InvoiceResponse;
import com.goodfunds.exception.InvoiceNotFoundException;
import com.goodfunds.invoice.parser.InvoiceParseException;
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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquestra o processamento de uma fatura ja enviada: extrai os lancamentos via
 * {@link InvoiceParserFactory}, gera as {@link Transaction}s correspondentes e atualiza
 * o estado da {@link Invoice}.
 */
@Service
public class InvoiceProcessingService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceProcessingService.class);

    /** Categoria padrao usada quando nao ha sugestao correspondente; semeada por usuario em {@code AuthService}. */
    private static final String DEFAULT_CATEGORY_NAME = "Outros";

    /** Limite de tamanho de {@code Transaction.descricao} (alinhado com a coluna {@code VARCHAR(500)}). */
    private static final int MAX_DESCRICAO_LENGTH = 500;

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final InvoiceParserFactory parserFactory;
    private final InvoiceUploadProperties uploadProperties;
    private final CategorySuggestionService categorySuggestionService;

    public InvoiceProcessingService(InvoiceRepository invoiceRepository,
                                    TransactionRepository transactionRepository,
                                    CategoryRepository categoryRepository,
                                    InvoiceParserFactory parserFactory,
                                    InvoiceUploadProperties uploadProperties,
                                    CategorySuggestionService categorySuggestionService) {
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.parserFactory = parserFactory;
        this.uploadProperties = uploadProperties;
        this.categorySuggestionService = categorySuggestionService;
    }

    @Transactional
    public InvoiceResponse process(UUID userId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (!invoice.getUser().getId().equals(userId)) {
            // Escopo por usuario: nao vaza a existencia de faturas de outros usuarios.
            throw new InvoiceNotFoundException(invoiceId);
        }

        if (invoice.getStatus() == StatusFatura.PROCESSADA) {
            // Idempotencia: fatura ja processada nao gera novas transactions.
            log.debug("Fatura {} ja processada; reprocessamento ignorado", invoiceId);
            return InvoiceResponse.from(invoice);
        }

        try {
            ParsedInvoice parsed = parse(invoice);
            // Valida os lancamentos antes de tocar o banco para que entradas invalidas
            // (ex.: linhas de credito/estorno com valor negativo) marquem ERRO de forma
            // controlada, em vez de estourar uma constraint no commit.
            validateLineItems(parsed);

            Map<String, Category> categoryByNome = buildCategoryMap(userId);
            Category defaultCategory = resolveDefaultCategory(userId, categoryByNome);

            // Idempotencia: limpa lancamentos de uma tentativa anterior antes de recriar.
            transactionRepository.deleteByInvoiceId(invoiceId);

            List<Transaction> transactions = parsed.transacoes().stream()
                    .map(item -> toTransaction(item, invoice, defaultCategory, categoryByNome))
                    .toList();
            transactionRepository.saveAll(transactions);

            invoice.setMesReferencia(parsed.mesReferencia());
            invoice.setTotalValor(parsed.total());
            invoice.setStatus(StatusFatura.PROCESSADA);
            log.info("Fatura {} processada: {} transactions geradas", invoiceId, transactions.size());
        } catch (RuntimeException ex) {
            // Falha de parse/validacao/resolucao de categoria marca a fatura como ERRO
            // sem propagar o rollback, permitindo nova tentativa.
            log.error("Falha ao processar fatura {}: {}", invoiceId, ex.getMessage(), ex);
            invoice.setStatus(StatusFatura.ERRO);
        }

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    private ParsedInvoice parse(Invoice invoice) {
        Path baseDir = Path.of(uploadProperties.getDir()).toAbsolutePath().normalize();
        Path resolved = baseDir.resolve(invoice.getArquivo()).normalize();
        if (!resolved.startsWith(baseDir)) {
            // Defesa contra path traversal caso `arquivo` deixe de ser gerado pelo servidor.
            throw new InvoiceParseException("Caminho de arquivo invalido para a fatura " + invoice.getId());
        }
        File pdf = resolved.toFile();
        return parserFactory.forInvoice(invoice).parse(pdf);
    }

    private void validateLineItems(ParsedInvoice parsed) {
        for (ParsedInvoiceTransaction item : parsed.transacoes()) {
            if (item.valor().signum() <= 0) {
                throw new InvoiceParseException(
                        "Lancamento com valor nao positivo (" + item.valor()
                                + ") nao e suportado: " + item.descricao());
            }
            String descricao = item.descricao() == null ? "" : item.descricao().strip();
            if (descricao.isEmpty()) {
                throw new InvoiceParseException("Lancamento sem descricao na fatura");
            }
            if (descricao.length() > MAX_DESCRICAO_LENGTH) {
                throw new InvoiceParseException(
                        "Descricao do lancamento excede " + MAX_DESCRICAO_LENGTH + " caracteres");
            }
        }
    }

    private Map<String, Category> buildCategoryMap(UUID userId) {
        return categoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(
                        c -> c.getNome().toLowerCase(Locale.ROOT),
                        c -> c,
                        (a, b) -> a));
    }

    private Category resolveDefaultCategory(UUID userId, Map<String, Category> categoryByNome) {
        Category defaultCategory = categoryByNome.get(DEFAULT_CATEGORY_NAME.toLowerCase(Locale.ROOT));
        if (defaultCategory == null) {
            throw new IllegalStateException(
                    "Categoria padrao '" + DEFAULT_CATEGORY_NAME
                            + "' nao encontrada para o usuario " + userId);
        }
        return defaultCategory;
    }

    private Transaction toTransaction(ParsedInvoiceTransaction item, Invoice invoice,
                                      Category defaultCategory, Map<String, Category> categoryByNome) {
        Category category = categorySuggestionService.suggest(item.descricao())
                .map(name -> categoryByNome.getOrDefault(name.toLowerCase(Locale.ROOT), defaultCategory))
                .orElse(defaultCategory);

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
