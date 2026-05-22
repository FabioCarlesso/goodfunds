package com.goodfunds.controller;

import com.goodfunds.dto.EstimateResponse;
import com.goodfunds.security.AuthenticatedUser;
import com.goodfunds.service.EstimateService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final EstimateService estimateService;

    public ReportController(EstimateService estimateService) {
        this.estimateService = estimateService;
    }

    @GetMapping("/estimate")
    public EstimateResponse estimate(@AuthenticationPrincipal AuthenticatedUser principal) {
        return estimateService.estimate(principal.getUserId());
    }
}
