package com.goodfunds.controller;

import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.dto.InvoiceResponse;
import com.goodfunds.security.AuthenticatedUser;
import com.goodfunds.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/invoices")
@Tag(name = "Faturas", description = "Upload de faturas em PDF para extracao de lancamentos.")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
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
}
