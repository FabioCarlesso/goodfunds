package com.goodfunds.dto;

import com.goodfunds.domain.FormaPagamento;
import com.goodfunds.domain.TipoCategoria;
import com.goodfunds.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        FormaPagamento formaPagamento,
        UUID categoryId,
        String categoryNome,
        TipoCategoria categoryTipo,
        UUID invoiceId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getDescricao(),
                tx.getValor(),
                tx.getData(),
                tx.getFormaPagamento(),
                tx.getCategory().getId(),
                tx.getCategory().getNome(),
                tx.getCategory().getTipo(),
                tx.getInvoice() != null ? tx.getInvoice().getId() : null,
                tx.getCreatedAt(),
                tx.getUpdatedAt()
        );
    }
}
