package com.goodfunds.invoice.parser;

import com.goodfunds.domain.OrigemFatura;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ItauInvoiceParserTest {

    private final ItauInvoiceParser parser = new ItauInvoiceParser();

    @Test
    void origem_isItau() {
        assertThat(parser.origem()).isEqualTo(OrigemFatura.ITAU);
    }

    @Test
    void parse_extractsTwoColumnLayout_skippingNegativesAndProjections(@TempDir Path tempDir)
            throws IOException {
        File pdf = ItauInvoiceFixtures.writeSamplePdf(tempDir.resolve("fatura.pdf")).toFile();

        ParsedInvoice parsed = parser.parse(pdf);

        // Mes de referencia vem do Vencimento (11/05/2026); total de "Total desta fatura".
        assertThat(parsed.mesReferencia()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(parsed.total()).isEqualByComparingTo(new BigDecimal("1234.56"));

        // Coluna esquerda + direita; estorno e pagamento (negativos) e projecoes excluidos.
        assertThat(parsed.transacoes())
                .extracting(ParsedInvoiceTransaction::data,
                        ParsedInvoiceTransaction::descricao,
                        ParsedInvoiceTransaction::valor)
                .containsExactlyInAnyOrder(
                        tuple(LocalDate.of(2025, 6, 1), "MERCADO LIVRE", new BigDecimal("89.90")),
                        tuple(LocalDate.of(2025, 6, 5), "PADARIA CENTRAL", new BigDecimal("24.80")),
                        tuple(LocalDate.of(2025, 6, 10), "NOTEBOOK", new BigDecimal("1500.00")),
                        tuple(LocalDate.of(2025, 6, 7), "CLAUDE AI SUBSCRIPTION", new BigDecimal("116.41")),
                        tuple(LocalDate.of(2025, 6, 2), "UBER TRIP", new BigDecimal("35.40")),
                        tuple(LocalDate.of(2025, 6, 11), "SUPERMERCADO", new BigDecimal("45.67"))
                );
    }

    @Test
    void parse_doesNotIncludeNegativeOrProjectedAmounts(@TempDir Path tempDir) throws IOException {
        File pdf = ItauInvoiceFixtures.writeSamplePdf(tempDir.resolve("fatura.pdf")).toFile();

        ParsedInvoice parsed = parser.parse(pdf);

        assertThat(parsed.transacoes()).hasSize(6);
        BigDecimal soma = parsed.transacoes().stream()
                .map(ParsedInvoiceTransaction::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 89,90 + 24,80 + 1.500,00 + 116,41 + 35,40 + 45,67
        assertThat(soma).isEqualByComparingTo(new BigDecimal("1812.18"));
        assertThat(parsed.transacoes())
                .noneMatch(t -> t.valor().signum() <= 0)
                .noneMatch(t -> t.descricao().contains("LOJA FUTURA"));
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
    void parse_failsWhenVencimentoMissing(@TempDir Path tempDir) throws IOException {
        File pdf = ItauInvoiceFixtures.writeLines(tempDir.resolve("f.pdf"), List.of(
                "Total desta fatura R$ 10,00",
                "Lancamentos: compras e saques",
                "01/06 MERCADO LIVRE 89,90"
        )).toFile();

        assertThatThrownBy(() -> parser.parse(pdf))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("Vencimento");
    }

    @Test
    void parse_failsWhenTotalMissing(@TempDir Path tempDir) throws IOException {
        File pdf = ItauInvoiceFixtures.writeLines(tempDir.resolve("f.pdf"), List.of(
                "Vencimento: 11/05/2026",
                "Lancamentos: compras e saques",
                "01/06 MERCADO LIVRE 89,90"
        )).toFile();

        assertThatThrownBy(() -> parser.parse(pdf))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("Total da fatura");
    }

    @Test
    void parse_failsWhenNoTransactions(@TempDir Path tempDir) throws IOException {
        File pdf = ItauInvoiceFixtures.writeLines(tempDir.resolve("f.pdf"), List.of(
                "Vencimento: 11/05/2026",
                "Total desta fatura R$ 10,00",
                "Lancamentos: compras e saques",
                "(sem lancamentos no formato esperado)"
        )).toFile();

        assertThatThrownBy(() -> parser.parse(pdf))
                .isInstanceOf(InvoiceParseException.class)
                .hasMessageContaining("Nenhum lancamento");
    }

    @Test
    void parse_recuaLancamentosPosterioresParaAnoAnterior(@TempDir Path tempDir) throws IOException {
        File pdf = ItauInvoiceFixtures.writeLines(tempDir.resolve("f.pdf"), List.of(
                "Vencimento: 11/01/2026",
                "Total desta fatura R$ 300,00",
                "Lancamentos: compras e saques",
                "20/12 COMPRA DEZEMBRO 200,00",
                "05/01 COMPRA JANEIRO 100,00"
        )).toFile();

        ParsedInvoice parsed = parser.parse(pdf);

        assertThat(parsed.transacoes())
                .extracting(ParsedInvoiceTransaction::data)
                .containsExactlyInAnyOrder(LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 5));
    }

    @Test
    void parse_handlesInstallmentMarkerInDescription(@TempDir Path tempDir) throws IOException {
        File pdf = ItauInvoiceFixtures.writeLines(tempDir.resolve("f.pdf"), List.of(
                "Vencimento: 11/06/2026",
                "Total desta fatura R$ 150,00",
                "Lancamentos: compras e saques",
                "10/06 LOJA X PARCELA 02/12 150,00"
        )).toFile();

        ParsedInvoice parsed = parser.parse(pdf);

        assertThat(parsed.transacoes()).singleElement()
                .satisfies(t -> {
                    assertThat(t.descricao()).isEqualTo("LOJA X PARCELA 02/12");
                    assertThat(t.valor()).isEqualByComparingTo(new BigDecimal("150.00"));
                });
    }
}
