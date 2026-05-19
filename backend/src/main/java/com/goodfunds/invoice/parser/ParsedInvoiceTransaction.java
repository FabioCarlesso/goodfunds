package com.goodfunds.invoice.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record ParsedInvoiceTransaction(
        LocalDate data,
        String descricao,
        BigDecimal valor
) {
    public ParsedInvoiceTransaction {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(descricao, "descricao");
        Objects.requireNonNull(valor, "valor");
    }
}
