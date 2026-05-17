package com.goodfunds.dto;

import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import com.goodfunds.domain.StatusFatura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String arquivo,
        OrigemFatura origem,
        StatusFatura status,
        YearMonth mesReferencia,
        BigDecimal totalValor,
        OffsetDateTime createdAt
) {
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getArquivo(),
                invoice.getOrigem(),
                invoice.getStatus(),
                invoice.getMesReferencia(),
                invoice.getTotalValor(),
                invoice.getCreatedAt()
        );
    }
}
