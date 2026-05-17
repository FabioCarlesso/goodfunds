package com.goodfunds.service;

import com.goodfunds.config.InvoiceUploadProperties;
import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.User;
import com.goodfunds.repository.InvoiceRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private UserRepository userRepository;

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
        InvoiceService service = new InvoiceService(invoiceRepository, userRepository, properties);
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

    private List<Path> regularFiles() throws IOException {
        try (var paths = Files.walk(uploadsDir)) {
            return paths.filter(Files::isRegularFile).toList();
        }
    }
}
