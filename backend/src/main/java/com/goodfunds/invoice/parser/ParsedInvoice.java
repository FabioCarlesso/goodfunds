package com.goodfunds.invoice.parser;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public record ParsedInvoice(
        YearMonth mesReferencia,
        BigDecimal total,
        List<ParsedInvoiceTransaction> transacoes
) {
    public ParsedInvoice {
        Objects.requireNonNull(mesReferencia, "mesReferencia");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(transacoes, "transacoes");
        transacoes = List.copyOf(transacoes);
    }
}
