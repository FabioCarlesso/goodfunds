/**
 * Parsers de faturas em PDF por origem.
 *
 * <p>Contem a {@link com.goodfunds.invoice.parser.InvoiceParser} (interface base),
 * implementacoes especificas por {@link com.goodfunds.domain.OrigemFatura} e a
 * {@link com.goodfunds.invoice.parser.InvoiceParserFactory} responsavel por
 * selecionar a implementacao adequada para cada {@link com.goodfunds.domain.Invoice}.</p>
 */
package com.goodfunds.invoice.parser;
