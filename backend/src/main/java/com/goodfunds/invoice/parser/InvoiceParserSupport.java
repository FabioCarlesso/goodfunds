package com.goodfunds.invoice.parser;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Base comum para os parsers de fatura.
 *
 * <p>Centraliza a conversao de valores no padrao brasileiro e a resolucao de
 * datas com recuo de ano, evitando duplicacao entre os parsers concretos
 * (ex.: {@link NubankInvoiceParser} e {@link ItauInvoiceParser}).</p>
 */
abstract class InvoiceParserSupport implements InvoiceParser {

    /**
     * Converte um valor no formato brasileiro ({@code 1.234,56}) para
     * {@link BigDecimal}, removendo o separador de milhar e normalizando a
     * virgula decimal.
     */
    protected BigDecimal parseValor(String raw) {
        String normalized = raw.replace(".", "").replace(',', '.');
        return new BigDecimal(normalized);
    }

    /**
     * Resolve a data de um lancamento aplicando o recuo de ano: quando o mes do
     * lancamento e posterior ao mes de referencia, assume-se o ano anterior
     * (ex.: lancamento de dezembro em fatura de janeiro).
     */
    protected LocalDate resolveDate(YearMonth mesReferencia, int month, int day) {
        int year = mesReferencia.getYear();
        if (month > mesReferencia.getMonthValue()) {
            year -= 1;
        }
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException ex) {
            throw new InvoiceParseException(
                    "Data invalida na fatura " + origem() + ": " + day + "/" + month + "/" + year, ex);
        }
    }
}
