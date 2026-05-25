package com.goodfunds.controller;

import com.goodfunds.config.OpenApiConfig;
import com.goodfunds.dto.ByCategoryItem;
import com.goodfunds.dto.EstimateResponse;
import com.goodfunds.dto.MonthlyEntry;
import com.goodfunds.dto.SummaryResponse;
import com.goodfunds.security.AuthenticatedUser;
import com.goodfunds.service.EstimateService;
import com.goodfunds.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/reports")
@Tag(name = "Relatorios", description = "Resumos, estimativas, gastos por categoria e evolucao mensal.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME_NAME)
public class ReportController {

    private final EstimateService estimateService;
    private final ReportService reportService;

    public ReportController(EstimateService estimateService, ReportService reportService) {
        this.estimateService = estimateService;
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo financeiro (receitas, despesas e saldo) do mes de referencia.")
    public SummaryResponse summary(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth ref) {
        return reportService.summary(principal.getUserId(), ref);
    }

    @GetMapping("/estimate")
    @Operation(summary = "Estimativa de fechamento do mes corrente com base nos gastos ja realizados.")
    public EstimateResponse estimate(@AuthenticationPrincipal AuthenticatedUser principal) {
        return estimateService.estimate(principal.getUserId());
    }

    @GetMapping("/by-category")
    @Operation(summary = "Gastos agrupados por categoria no mes de referencia.")
    public List<ByCategoryItem> byCategory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth ref) {
        return reportService.byCategory(principal.getUserId(), ref);
    }

    @GetMapping("/evolution")
    @Operation(summary = "Evolucao mensal de receitas e despesas entre dois meses (from/to).")
    public List<MonthlyEntry> evolution(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return reportService.evolution(principal.getUserId(), from, to);
    }
}
