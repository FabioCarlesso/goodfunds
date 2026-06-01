package com.goodfunds.service;

import com.goodfunds.config.InvoiceUploadProperties;
import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.domain.StatusFatura;
import com.goodfunds.domain.User;
import com.goodfunds.exception.InvoiceNotFoundException;
import com.goodfunds.repository.InvoiceRepository;
import com.goodfunds.repository.TransactionRepository;
import com.goodfunds.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ReportCacheService reportCacheService;

    @TempDir
    Path uploadsDir;

    @Test
    void upload_whenPersistenceFails_removesSavedFile() throws IOException {
        User user = User.builder()
                .id(UUID.randomUUID())
                .nome("Owner")
                .email("owner@example.com")
                .senha("hash")
                .build();
        InvoiceUploadProperties properties = new InvoiceUploadProperties();
        properties.setDir(uploadsDir.toString());
        InvoiceService service = new InvoiceService(
                invoiceRepository, userRepository, transactionRepository, properties, reportCacheService);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fatura.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 conteudo".getBytes());

        when(userRepository.getReferenceById(user.getId())).thenReturn(user);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            assertThat(regularFiles()).hasSize(1);
            throw new RuntimeException("database unavailable");
        });

        assertThatThrownBy(() -> service.upload(user.getId(), file, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("database unavailable");

        assertThat(regularFiles()).isEmpty();
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void delete_removesTransactionsInvoiceFileAndEvictsCache() throws IOException {
        UUID userId = UUID.randomUUID();
        String relativePath = userId + "/fatura.pdf";
        Path file = uploadsDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, "%PDF-1.4 conteudo".getBytes());

        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .arquivo(relativePath)
                .origem(OrigemFatura.NUBANK)
                .status(StatusFatura.PROCESSADA)
                .build();

        InvoiceService service = newService();
        when(invoiceRepository.findByIdAndUserId(invoice.getId(), userId))
                .thenReturn(Optional.of(invoice));

        service.delete(userId, invoice.getId());

        // Sem transacao ativa (teste puro), o arquivo e removido imediatamente.
        assertThat(Files.exists(file)).isFalse();
        verify(transactionRepository).deleteByInvoiceId(invoice.getId());
        verify(invoiceRepository).delete(invoice);
        verify(reportCacheService).evictUser(userId);
    }

    @Test
    void delete_unknownInvoice_throwsNotFoundAndTouchesNothing() {
        UUID userId = UUID.randomUUID();
        UUID missing = UUID.randomUUID();

        InvoiceService service = newService();
        when(invoiceRepository.findByIdAndUserId(missing, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, missing))
                .isInstanceOf(InvoiceNotFoundException.class);

        verify(invoiceRepository, never()).delete(any(Invoice.class));
        verifyNoInteractions(transactionRepository, reportCacheService);
    }

    private InvoiceService newService() {
        InvoiceUploadProperties properties = new InvoiceUploadProperties();
        properties.setDir(uploadsDir.toString());
        return new InvoiceService(
                invoiceRepository, userRepository, transactionRepository, properties, reportCacheService);
    }

    private List<Path> regularFiles() throws IOException {
        try (var paths = Files.walk(uploadsDir)) {
            return paths.filter(Files::isRegularFile).toList();
        }
    }
}
