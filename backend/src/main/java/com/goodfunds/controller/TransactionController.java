package com.goodfunds.controller;

import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.dto.TransactionCategoryRequest;
import com.goodfunds.dto.TransactionRequest;
import com.goodfunds.dto.TransactionResponse;
import com.goodfunds.security.AuthenticatedUser;
import com.goodfunds.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transacoes", description = "Lancamentos financeiros: busca paginada com filtros, criacao, edicao e exclusao.")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "Busca paginada de transacoes com filtros por mes, categoria, tipo e periodo.")
    public Page<TransactionResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth ref,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) TipoCategoria tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.search(principal.getUserId(), ref, categoryId, tipo, from, to, pageable);
    }

    @PostMapping
    @Operation(summary = "Cria uma nova transacao.")
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.create(principal.getUserId(), request);
        URI location = UriComponentsBuilder.fromPath("/transactions/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma transacao existente.")
    public ResponseEntity<TransactionResponse> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(principal.getUserId(), id, request));
    }

    @PatchMapping("/{id}/category")
    @Operation(summary = "Reclassifica a categoria de uma transacao.")
    public ResponseEntity<TransactionResponse> updateCategory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody TransactionCategoryRequest request) {
        return ResponseEntity.ok(transactionService.updateCategory(principal.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma transacao.")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id) {
        transactionService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
