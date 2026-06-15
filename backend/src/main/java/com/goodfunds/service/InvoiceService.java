package com.goodfunds.service;

import com.goodfunds.config.InvoiceUploadProperties;
import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.domain.StatusFatura;
import com.goodfunds.domain.User;
import com.goodfunds.dto.InvoiceDetailResponse;
import com.goodfunds.dto.InvoiceResponse;
import com.goodfunds.dto.TransactionResponse;
import com.goodfunds.exception.InvalidInvoiceFileException;
import com.goodfunds.exception.InvoiceNotFoundException;
import com.goodfunds.exception.UnsupportedInvoiceOrigemException;
import com.goodfunds.invoice.parser.InvoiceParserFactory;
import com.goodfunds.repository.InvoiceRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};
    private static final String PDF_EXTENSION = ".pdf";

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceUploadProperties uploadProperties;
    private final ReportCacheService reportCacheService;
    private final InvoiceParserFactory parserFactory;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          UserRepository userRepository,
                          TransactionRepository transactionRepository,
                          InvoiceUploadProperties uploadProperties,
                          ReportCacheService reportCacheService,
                          InvoiceParserFactory parserFactory) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.uploadProperties = uploadProperties;
        this.reportCacheService = reportCacheService;
        this.parserFactory = parserFactory;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listByUser(UUID userId) {
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDetailResponse getById(UUID userId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        List<TransactionResponse> transactions = transactionRepository
                .findByInvoiceIdOrderByDataAsc(invoiceId).stream()
                .map(TransactionResponse::from)
                .toList();
        return InvoiceDetailResponse.from(invoice, transactions);
    }

    @Transactional
    public InvoiceResponse upload(UUID userId, MultipartFile file, OrigemFatura origem) {
        validateFile(file);
        OrigemFatura origemFinal = origem != null ? origem : OrigemFatura.NUBANK;
        validateOrigemSuportada(origemFinal);

        Path baseDir = Path.of(uploadProperties.getDir()).toAbsolutePath().normalize();
        Path userDir = baseDir.resolve(userId.toString());
        String filename = UUID.randomUUID() + PDF_EXTENSION;
        Path target = userDir.resolve(filename);

        try {
            Files.createDirectories(userDir);
            file.transferTo(target);
        } catch (IOException ex) {
            log.error("Falha ao salvar fatura para usuario {}", userId, ex);
            throw new IllegalStateException("Falha ao salvar arquivo da fatura", ex);
        }

        registerRollbackCleanup(target);

        try {
            String relativePath = userId + "/" + filename;
            User userRef = userRepository.getReferenceById(userId);
            Invoice invoice = Invoice.builder()
                    .arquivo(relativePath)
                    .origem(origemFinal)
                    .status(StatusFatura.PENDENTE_PARSE)
                    .user(userRef)
                    .build();

            Invoice saved = invoiceRepository.save(invoice);
            return InvoiceResponse.from(saved);
        } catch (RuntimeException ex) {
            deleteFileIfExists(target);
            throw ex;
        }
    }

    @Transactional
    public void delete(UUID userId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Remove primeiro as transactions geradas pela fatura para nao violar a FK.
        transactionRepository.deleteByInvoiceId(invoiceId);
        invoiceRepository.delete(invoice);

        // So apaga o PDF apos o commit: se a transacao reverter, o arquivo continua
        // referenciado pela fatura ainda existente.
        Path file = resolveStoredFile(invoice.getArquivo());
        if (file != null) {
            registerCommitCleanup(file);
        }

        // A remocao das transactions altera os agregados; invalida os relatorios cacheados.
        reportCacheService.evictUser(userId);
    }

    private Path resolveStoredFile(String relativePath) {
        Path baseDir = Path.of(uploadProperties.getDir()).toAbsolutePath().normalize();
        Path resolved = baseDir.resolve(relativePath).normalize();
        if (!resolved.startsWith(baseDir)) {
            // Defesa contra path traversal caso `arquivo` deixe de ser gerado pelo servidor.
            log.warn("Caminho de arquivo invalido ao excluir fatura: {}", relativePath);
            return null;
        }
        return resolved;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidInvoiceFileException("Arquivo da fatura e obrigatorio");
        }
        String contentType = file.getContentType();
        if (contentType == null || !MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType)) {
            throw new InvalidInvoiceFileException("Arquivo deve ser PDF (application/pdf)");
        }
        if (!hasPdfSignature(file)) {
            throw new InvalidInvoiceFileException("Arquivo nao e um PDF valido");
        }
    }

    private void validateOrigemSuportada(OrigemFatura origem) {
        if (parserFactory.suporta(origem)) {
            return;
        }
        String suportadas = parserFactory.origensSuportadas().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(", "));
        throw new UnsupportedInvoiceOrigemException(
                "Origem de fatura nao suportada: " + origem
                        + ". Origens suportadas: " + suportadas);
    }

    private boolean hasPdfSignature(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(PDF_MAGIC.length);
            if (header.length < PDF_MAGIC.length) {
                return false;
            }
            for (int i = 0; i < PDF_MAGIC.length; i++) {
                if (header[i] != PDF_MAGIC[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException ex) {
            throw new InvalidInvoiceFileException("Nao foi possivel ler o arquivo enviado");
        }
    }

    private void registerRollbackCleanup(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteFileIfExists(target);
                }
            }
        });
    }

    private void registerCommitCleanup(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Sem transacao ativa (ex.: chamada direta em teste): remove imediatamente.
            deleteFileIfExists(target);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    deleteFileIfExists(target);
                }
            }
        });
    }

    private void deleteFileIfExists(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException cleanupEx) {
            log.warn("Falha ao remover fatura apos rollback: {}", target, cleanupEx);
        }
    }

}
