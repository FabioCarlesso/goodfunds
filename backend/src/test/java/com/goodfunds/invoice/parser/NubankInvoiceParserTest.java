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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NubankInvoiceParserTest {

    private final NubankInvoiceParser parser = new NubankInvoiceParser();

    @Test
    void origem_isNubank() {
        assertThat(parser.origem()).isEqualTo(OrigemFatura.NUBANK);
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

    private File copyFixtureTo(Path target) throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream(NubankInvoiceFixtures.RESOURCE_PATH)) {
            assertThat(in)
                    .as("fixture %s no classpath", NubankInvoiceFixtures.RESOURCE_PATH)
                    .isNotNull();
            Files.copy(in, target);
        }
        return target.toFile();
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.api.Assertions.tuple(values);
    }
}
