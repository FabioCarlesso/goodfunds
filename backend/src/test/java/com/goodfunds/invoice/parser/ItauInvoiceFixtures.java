package com.goodfunds.invoice.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Gera uma fixture PDF sintetica que reproduz o leiaute de <strong>duas colunas</strong>
 * da fatura real do Itau, com dados ficticios. Cobre os casos que o parser precisa tratar:
 *
 * <ul>
 *   <li>mes de referencia derivado de {@code Vencimento};</li>
 *   <li>{@code Total desta fatura} sem {@code R$} colado;</li>
 *   <li>duas colunas de lancamentos na mesma linha fisica (esquerda/direita);</li>
 *   <li>separador de milhar a nivel de lancamento ({@code 1.500,00});</li>
 *   <li>linha de pagamento negativa antes da secao de compras (ignorada);</li>
 *   <li>estorno negativo dentro da secao (ignorado);</li>
 *   <li>lancamento internacional (incluido);</li>
 *   <li>tabela {@code Compras parceladas - proximas faturas} (excluida).</li>
 * </ul>
 *
 * <p>Os textos sao posicionados em coordenadas absolutas para que o
 * {@code PDFTextStripper} mescle as colunas exatamente como numa fatura real.</p>
 */
public final class ItauInvoiceFixtures {

    private static final float LEFT_X = 50f;
    private static final float RIGHT_X = 390f;

    private ItauInvoiceFixtures() {
    }

    public static Path writeSamplePdf(Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (PDDocument document = new PDDocument()) {
            writePage1(document);
            writePage2(document);
            document.save(target.toFile());
        }
        return target;
    }

    private static void writePage1(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        try (PDPageContentStream c = open(document, page)) {
            text(c, LEFT_X, 760, "Vencimento: 11/05/2026");
            text(c, LEFT_X, 745, "Total desta fatura R$ 1.234,56");
            text(c, LEFT_X, 720, "Pagamentos efetuados");
            text(c, LEFT_X, 705, "09/04 PAGAMENTO FATURA ANTERIOR -500,00");
            text(c, LEFT_X, 685, "Lancamentos: compras e saques");
            // Linhas de duas colunas (esquerda + direita no mesmo y).
            text(c, LEFT_X, 670, "01/06 MERCADO LIVRE 89,90");
            text(c, RIGHT_X, 670, "02/06 UBER TRIP 35,40");
            text(c, LEFT_X, 655, "05/06 PADARIA CENTRAL 24,80");
            text(c, RIGHT_X, 655, "06/06 ESTORNO LOJA -10,00");
            text(c, LEFT_X, 635, "10/06 NOTEBOOK 1.500,00");
            text(c, RIGHT_X, 635, "11/06 SUPERMERCADO 45,67");
            text(c, LEFT_X, 615, "Lancamentos internacionais");
            text(c, LEFT_X, 600, "07/06 CLAUDE AI SUBSCRIPTION 116,41");
        }
    }

    private static void writePage2(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        try (PDPageContentStream c = open(document, page)) {
            text(c, LEFT_X, 760, "Total dos lancamentos atuais 1.811,38");
            text(c, LEFT_X, 740, "Compras parceladas - proximas faturas");
            text(c, LEFT_X, 725, "01/06 MERCADO LIVRE 02/10 89,90");
            text(c, LEFT_X, 710, "08/07 LOJA FUTURA 200,00");
        }
    }

    /** Permite gerar PDFs de uma coluna a partir de linhas arbitrarias (casos de borda). */
    public static Path writeLines(Path target, List<String> lines) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            try (PDPageContentStream c = open(document, page)) {
                float y = 760;
                for (String line : lines) {
                    text(c, LEFT_X, y, line);
                    y -= 16;
                }
            }
            document.save(target.toFile());
        }
        return target;
    }

    private static PDPageContentStream open(PDDocument document, PDPage page) throws IOException {
        PDPageContentStream c = new PDPageContentStream(document, page);
        c.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
        return c;
    }

    private static void text(PDPageContentStream c, float x, float y, String value) throws IOException {
        c.beginText();
        c.newLineAtOffset(x, y);
        c.showText(value);
        c.endText();
    }
}
