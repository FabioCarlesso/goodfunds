package com.goodfunds.dto;

import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.domain.StatusFatura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record InvoiceDetailResponse(
        UUID id,
        String arquivo,
        OrigemFatura origem,
        StatusFatura status,
        YearMonth mesReferencia,
        BigDecimal totalValor,
        OffsetDateTime createdAt,
        List<TransactionResponse> transactions
) {
    public static InvoiceDetailResponse from(Invoice invoice, List<TransactionResponse> transactions) {
        return new InvoiceDetailResponse(
                invoice.getId(),
                invoice.getArquivo(),
                invoice.getOrigem(),
                invoice.getStatus(),
                invoice.getMesReferencia(),
                invoice.getTotalValor(),
                invoice.getCreatedAt(),
                transactions
        );
    }
}
