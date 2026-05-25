package com.goodfunds.controller;

import com.goodfunds.config.OpenApiConfig;
import com.goodfunds.dto.BudgetRequest;
import com.goodfunds.dto.BudgetResponse;
import com.goodfunds.security.AuthenticatedUser;
import com.goodfunds.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/budgets")
@Tag(name = "Orcamentos", description = "Planejamento financeiro: orcamentos mensais por categoria.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "Lista os orcamentos do mes de referencia (yyyy-MM).")
    public List<BudgetResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth ref) {
        return budgetService.list(principal.getUserId(), ref);
    }

    @PostMapping
    @Operation(summary = "Cria um orcamento para uma categoria em um mes.")
    public ResponseEntity<BudgetResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.create(principal.getUserId(), request);
        URI location = UriComponentsBuilder.fromPath("/budgets/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o valor de um orcamento existente.")
    public ResponseEntity<BudgetResponse> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.update(principal.getUserId(), id, request));
    }
}
