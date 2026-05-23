package com.goodfunds.controller;

import com.goodfunds.dto.ByCategoryItem;
import com.goodfunds.dto.EstimateResponse;
import com.goodfunds.dto.MonthlyEntry;
import com.goodfunds.security.AuthenticatedUser;
import com.goodfunds.service.EstimateService;
import com.goodfunds.service.ReportService;
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
public class ReportController {

    private final EstimateService estimateService;
    private final ReportService reportService;

    public ReportController(EstimateService estimateService, ReportService reportService) {
        this.estimateService = estimateService;
        this.reportService = reportService;
    }

    @GetMapping("/estimate")
    public EstimateResponse estimate(@AuthenticationPrincipal AuthenticatedUser principal) {
        return estimateService.estimate(principal.getUserId());
    }

    @GetMapping("/by-category")
    public List<ByCategoryItem> byCategory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth ref) {
        return reportService.byCategory(principal.getUserId(), ref);
    }

    @GetMapping("/evolution")
    public List<MonthlyEntry> evolution(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return reportService.evolution(principal.getUserId(), from, to);
    }
}
