package com.goodfunds.invoice.parser;

import com.goodfunds.domain.OrigemFatura;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItauInvoiceParserTest {

    private final ItauInvoiceParser parser = new ItauInvoiceParser();

    @Test
    void origem_isItau() {
        assertThat(parser.origem()).isEqualTo(OrigemFatura.ITAU);
    }

    @Test
    void parse_extractsMonthTotalAndTransactionsFromFixture(@TempDir Path tempDir) throws IOException {
        File pdf = copyFixtureTo(tempDir.resolve("fatura.pdf"));

        ParsedInvoice parsed = parser.parse(pdf);

        assertThat(parsed.mesReferencia()).isEqualTo(YearMonth.of(2025, 6));
        assertThat(parsed.total()).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(parsed.transacoes())
                .extracting(ParsedInvoiceTransaction::data,
                        ParsedInvoiceTransaction::descricao,
                        ParsedInvoiceTransaction::valor)
                .containsExactly(
                        tuple(LocalDate.of(2025, 6, 1), "MERCADO LIVRE", new BigDecimal("89.90")),
                        tuple(LocalDate.of(2025, 6, 5), "UBER TRIP", new BigDecimal("35.40")),
                        tuple(LocalDate.of(2025, 6, 12), "PADARIA CENTRAL", new BigDecimal("24.80")),
                        tuple(LocalDate.of(2025, 5, 28), "NETFLIX", new BigDecimal("55.90")),
                        tuple(LocalDate.of(2025, 5, 30), "POSTO SHELL", new BigDecimal("200.00"))
                );
    }

    @Test
    void parse_failsWhenFileMissing(@TempDir Path tempDir) {
        File missing = tempDir.resolve("nao-existe.pdf").toFile();

        assertThatThrownBy(() -> parser.parse(missing))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("inacessivel");
    }

    @Test
    void parse_failsWhenFileNotPdf(@TempDir Path tempDir) throws IOException {
        Path notPdf = tempDir.resolve("texto.pdf");
        Files.writeString(notPdf, "isso nao e um pdf");

        assertThatThrownBy(() -> parser.parse(notPdf.toFile()))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("Falha ao ler PDF");
    }

    @Test
    void parse_failsWhenPdfIsNull() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("obrigatorio");
    }

    @Test
    void parse_inaccessibleFileMessageDoesNotLeakPath(@TempDir Path tempDir) {
        File missing = tempDir.resolve("subdir/secret-token.pdf").toFile();

        assertThatThrownBy(() -> parser.parse(missing))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("secret-token.pdf")
                .hasMessageNotContaining(tempDir.toString());
    }

    @Test
    void parseText_failsWhenMonthMissing() {
        String text = String.join("\n",
                "Total desta fatura R$ 1.234,56",
                "01/06 MERCADO LIVRE 89,90");

        assertThatThrownBy(() -> parser.parseText(text))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("Mes de referencia");
    }

    @Test
    void parseText_failsWhenTotalMissing() {
        String text = String.join("\n",
                "Mes de referencia: 06/2025",
                "01/06 MERCADO LIVRE 89,90");

        assertThatThrownBy(() -> parser.parseText(text))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("Total da fatura");
    }

    @Test
    void parseText_failsWhenNoTransactions() {
        String text = String.join("\n",
                "Mes de referencia: 06/2025",
                "Total desta fatura R$ 1.234,56",
                "(sem lancamentos no formato esperado)");

        assertThatThrownBy(() -> parser.parseText(text))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("Nenhum lancamento");
    }

    @Test
    void parseText_failsWhenDayIsInvalid() {
        String text = String.join("\n",
                "Mes de referencia: 02/2025",
                "Total desta fatura R$ 100,00",
                "31/02 TRANSACAO INVALIDA 10,00");

        assertThatThrownBy(() -> parser.parseText(text))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("Data invalida");
    }

    @Test
    void parseText_handlesDescriptionContainingInstallments() {
        String text = String.join("\n",
                "Mes de referencia: 06/2025",
                "Total desta fatura R$ 150,00",
                "10/06 LOJA X PARCELA 02/12 150,00");

        ParsedInvoice parsed = parser.parseText(text);

        assertThat(parsed.transacoes()).singleElement()
                .satisfies(t -> {
                    assertThat(t.data()).isEqualTo(LocalDate.of(2025, 6, 10));
                    assertThat(t.descricao()).isEqualTo("LOJA X PARCELA 02/12");
                    assertThat(t.valor()).isEqualByComparingTo(new BigDecimal("150.00"));
                });
    }

    @Test
    void parseText_handlesNegativeTotalAndValue() {
        String text = String.join("\n",
                "Mes de referencia: 06/2025",
                "Total desta fatura R$ -50,00",
                "05/06 ESTORNO -50,00");

        ParsedInvoice parsed = parser.parseText(text);

        assertThat(parsed.total()).isEqualByComparingTo(new BigDecimal("-50.00"));
        assertThat(parsed.transacoes()).singleElement()
                .extracting(ParsedInvoiceTransaction::valor)
                .isEqualTo(new BigDecimal("-50.00"));
    }

    @Test
    void parseText_recuaTransacoesPosterioresParaAnoAnterior() {
        String text = String.join("\n",
                "Mes de referencia: 01/2025",
                "Total desta fatura R$ 300,00",
                "20/12 COMPRA DEZEMBRO 200,00",
                "05/01 COMPRA JANEIRO 100,00");

        ParsedInvoice parsed = parser.parseText(text);

        assertThat(parsed.transacoes())
                .extracting(ParsedInvoiceTransaction::data)
                .containsExactly(LocalDate.of(2024, 12, 20), LocalDate.of(2025, 1, 5));
    }

    @Test
    void parseText_endToEndAgainstPdfGeneratedFromLines(@TempDir Path tempDir) throws IOException {
        Path pdf = tempDir.resolve("custom.pdf");
        ItauInvoiceFixtures.writePdf(pdf, List.of(
                "Mes de referencia: 03/2025",
                "Total desta fatura R$ 10,00",
                "01/03 ITEM UNICO 10,00"
        ));

        ParsedInvoice parsed = parser.parse(pdf.toFile());

        assertThat(parsed.mesReferencia()).isEqualTo(YearMonth.of(2025, 3));
        assertThat(parsed.transacoes()).hasSize(1);
    }

    private File copyFixtureTo(Path target) throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream(ItauInvoiceFixtures.RESOURCE_PATH)) {
            assertThat(in)
                    .as("fixture %s no classpath", ItauInvoiceFixtures.RESOURCE_PATH)
                    .isNotNull();
            Files.copy(in, target);
        }
        return target.toFile();
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.api.Assertions.tuple(values);
    }
}
