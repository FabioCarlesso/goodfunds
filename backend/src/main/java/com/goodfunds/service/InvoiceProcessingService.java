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
import java.util.ArrayList;
import java.util.Comparator;
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
    private final ReportCacheService reportCacheService;

    public InvoiceProcessingService(InvoiceRepository invoiceRepository,
                                    TransactionRepository transactionRepository,
                                    CategoryRepository categoryRepository,
                                    InvoiceParserFactory parserFactory,
                                    InvoiceUploadProperties uploadProperties,
                                    CategorySuggestionService categorySuggestionService,
                                    ReportCacheService reportCacheService) {
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.parserFactory = parserFactory;
        this.uploadProperties = uploadProperties;
        this.categorySuggestionService = categorySuggestionService;
        this.reportCacheService = reportCacheService;
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
            // Seleciona os lancamentos que viram transactions antes de tocar o banco:
            // linhas de pagamento/estorno (valor nao positivo) sao ignoradas e as demais
            // sao validadas, para que faturas reais cheguem a PROCESSADA sem estourar
            // constraint no commit.
            List<ParsedInvoiceTransaction> lineItems = selectPersistableLineItems(parsed);

            Map<String, Category> categoryByNome = buildCategoryMap(userId);
            Category defaultCategory = resolveDefaultCategory(userId, categoryByNome);

            // Idempotencia: limpa lancamentos de uma tentativa anterior antes de recriar.
            transactionRepository.deleteByInvoiceId(invoiceId);

            List<Transaction> transactions = lineItems.stream()
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

        // O processamento pode ter criado ou removido transactions (inclusive na limpeza
        // anterior ao marcar ERRO), entao invalida os relatorios cacheados do usuario.
        reportCacheService.evictUser(userId);
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

    /**
     * Seleciona os lancamentos que devem virar {@link Transaction}, aplicando a regra de
     * produto para valores negativos (issue #72).
     *
     * <p>Linhas com valor <strong>nao positivo</strong> — pagamentos da fatura anterior e
     * estornos/creditos, presentes em praticamente toda fatura real (Nubank e Itau) — sao
     * <strong>ignoradas</strong>: nao geram transaction. Isso permite que a fatura chegue a
     * {@code PROCESSADA} em vez de cair em {@code ERRO}, mantem os relatorios como somatorio
     * dos gastos do periodo (estornos nao reduzem artificialmente o total) e respeita o
     * invariante da coluna {@code valor} ({@code @Positive} / {@code CHECK (valor > 0)}), que
     * nao aceita persistir valores negativos. A regra e aplicada no servico, valendo de forma
     * identica para todas as origens.
     *
     * <p>Os lancamentos mantidos (positivos) ainda sao validados quanto a descricao (nao vazia
     * e ate {@value #MAX_DESCRICAO_LENGTH} caracteres); uma descricao invalida marca a fatura
     * como {@code ERRO} de forma controlada.
     */
    private List<ParsedInvoiceTransaction> selectPersistableLineItems(ParsedInvoice parsed) {
        List<ParsedInvoiceTransaction> persistable = new ArrayList<>();
        for (ParsedInvoiceTransaction item : parsed.transacoes()) {
            if (item.valor().signum() <= 0) {
                // Pagamento/estorno: ignorado, nao gera transaction.
                log.debug("Lancamento ignorado por valor nao positivo ({}): {}",
                        item.valor(), item.descricao());
                continue;
            }
            String descricao = item.descricao() == null ? "" : item.descricao().strip();
            if (descricao.isEmpty()) {
                throw new InvoiceParseException("Lancamento sem descricao na fatura");
            }
            if (descricao.length() > MAX_DESCRICAO_LENGTH) {
                throw new InvoiceParseException(
                        "Descricao do lancamento excede " + MAX_DESCRICAO_LENGTH + " caracteres");
            }
            persistable.add(item);
        }
        return persistable;
    }

    private Map<String, Category> buildCategoryMap(UUID userId) {
        // Ordena por id antes de deduplicar para que, havendo categorias com o mesmo nome
        // (case-insensitive), o vencedor seja deterministico (menor id) — mantendo a
        // garantia anterior do resolveDefaultCategory via `...OrderByIdAsc`.
        return categoryRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Category::getId))
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
