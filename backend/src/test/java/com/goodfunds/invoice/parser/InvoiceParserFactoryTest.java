package com.goodfunds.invoice.parser;

import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceParserFactoryTest {

    @Test
    void forInvoice_returnsParserMatchingOrigem() {
        InvoiceParser nubank = new NubankInvoiceParser();
        InvoiceParser itau = stubParser(OrigemFatura.ITAU);
        InvoiceParserFactory factory = new InvoiceParserFactory(List.of(nubank, itau));

        Invoice invoice = Invoice.builder().origem(OrigemFatura.ITAU).build();
        assertThat(factory.forInvoice(invoice)).isSameAs(itau);
        assertThat(factory.forOrigem(OrigemFatura.NUBANK)).isSameAs(nubank);
    }

    @Test
    void resolvesRealParsersForNubankAndItau() {
        InvoiceParser nubank = new NubankInvoiceParser();
        InvoiceParser itau = new ItauInvoiceParser();
        InvoiceParserFactory factory = new InvoiceParserFactory(List.of(nubank, itau));

        assertThat(factory.forOrigem(OrigemFatura.NUBANK)).isSameAs(nubank);
        assertThat(factory.forOrigem(OrigemFatura.ITAU)).isSameAs(itau);
    }

    @Test
    void exposesSupportedOrigens() {
        InvoiceParserFactory factory = new InvoiceParserFactory(
                List.of(new NubankInvoiceParser(), new ItauInvoiceParser()));

        assertThat(factory.origensSuportadas())
                .containsExactlyInAnyOrder(OrigemFatura.NUBANK, OrigemFatura.ITAU);
        assertThat(factory.suporta(OrigemFatura.NUBANK)).isTrue();
        assertThat(factory.suporta(OrigemFatura.ITAU)).isTrue();
        assertThat(factory.suporta(OrigemFatura.OUTROS)).isFalse();
        assertThat(factory.suporta(null)).isFalse();
    }

    @Test
    void forOrigem_failsWhenNoParserRegistered() {
        InvoiceParserFactory factory = new InvoiceParserFactory(List.of(new NubankInvoiceParser()));

        assertThatThrownBy(() -> factory.forOrigem(OrigemFatura.OUTROS))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("OUTROS");
    }

    @Test
    void constructor_failsOnDuplicateOrigem() {
        InvoiceParser one = stubParser(OrigemFatura.NUBANK);
        InvoiceParser other = stubParser(OrigemFatura.NUBANK);

        assertThatThrownBy(() -> new InvoiceParserFactory(List.of(one, other)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NUBANK");
    }

    private InvoiceParser stubParser(OrigemFatura origem) {
        return new InvoiceParser() {
            @Override
            public OrigemFatura origem() {
                return origem;
            }

            @Override
            public ParsedInvoice parse(File pdf) {
                return new ParsedInvoice(YearMonth.of(2025, 1), BigDecimal.ZERO, List.of());
            }
        };
    }
}
