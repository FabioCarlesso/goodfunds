package com.goodfunds.controller;

import com.goodfunds.config.OpenApiConfig;
import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.dto.InvoiceDetailResponse;
import com.goodfunds.dto.InvoiceResponse;
import com.goodfunds.security.AuthenticatedUser;
import com.goodfunds.service.InvoiceProcessingService;
import com.goodfunds.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/invoices")
@Tag(name = "Faturas", description = "Upload de faturas em PDF para extracao de lancamentos.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceProcessingService invoiceProcessingService;

    public InvoiceController(InvoiceService invoiceService,
                             InvoiceProcessingService invoiceProcessingService) {
        this.invoiceService = invoiceService;
        this.invoiceProcessingService = invoiceProcessingService;
    }

    @GetMapping
    @Operation(summary = "Lista as faturas do usuario autenticado, ordenadas pela data de importacao.")
    public List<InvoiceResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return invoiceService.listByUser(principal.getUserId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retorna a fatura e as transacoes geradas a partir dela.")
    public InvoiceDetailResponse get(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id) {
        return invoiceService.getById(principal.getUserId(), id);
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Faz upload de uma fatura PDF e registra a fatura para processamento.")
    public ResponseEntity<InvoiceResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "origem", required = false) OrigemFatura origem) {
        InvoiceResponse response = invoiceService.upload(principal.getUserId(), file, origem);
        URI location = UriComponentsBuilder.fromPath("/invoices/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Processa uma fatura pendente: extrai os lancamentos do PDF e gera as transacoes.")
    public InvoiceResponse process(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id) {
        return invoiceProcessingService.process(principal.getUserId(), id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma fatura, o arquivo PDF e as transacoes geradas a partir dela.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id) {
        invoiceService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
