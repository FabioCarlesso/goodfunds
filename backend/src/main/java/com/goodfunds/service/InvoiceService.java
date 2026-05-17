package com.goodfunds.service;

import com.goodfunds.config.InvoiceUploadProperties;
import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.domain.StatusFatura;
import com.goodfunds.domain.User;
import com.goodfunds.dto.InvoiceResponse;
import com.goodfunds.exception.InvalidInvoiceFileException;
import com.goodfunds.repository.InvoiceRepository;
import com.goodfunds.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};
    private static final String PDF_EXTENSION = ".pdf";

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final InvoiceUploadProperties uploadProperties;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          UserRepository userRepository,
                          InvoiceUploadProperties uploadProperties) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.uploadProperties = uploadProperties;
    }

    @Transactional
    public InvoiceResponse upload(UUID userId, MultipartFile file, OrigemFatura origem) {
        validateFile(file);
        OrigemFatura origemFinal = origem != null ? origem : OrigemFatura.NUBANK;

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

}
