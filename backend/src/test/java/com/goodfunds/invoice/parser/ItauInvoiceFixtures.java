package com.goodfunds.invoice.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Gera a fixture PDF da fatura Itau usada nos testes.
 *
 * <p>O metodo {@link #main(String[])} grava a fixture em
 * {@code src/test/resources/invoices/itau/sample-fatura.pdf}, permitindo
 * regenerar o arquivo deterministicamente sempre que o formato esperado pelo
 * parser mudar.</p>
 */
public final class ItauInvoiceFixtures {

    public static final String RESOURCE_PATH = "invoices/itau/sample-fatura.pdf";

    public static final List<String> SAMPLE_LINES = List.of(
            "Itau Cartao de Credito",
            "Mes de referencia: 06/2025",
            "Vencimento: 10/07/2025",
            "Total desta fatura R$ 1.234,56",
            "",
            "Lancamentos",
            "01/06 MERCADO LIVRE 89,90",
            "05/06 UBER TRIP 35,40",
            "12/06 PADARIA CENTRAL 24,80",
            "28/05 NETFLIX 55,90",
            "30/05 POSTO SHELL 200,00"
    );

    private ItauInvoiceFixtures() {
    }

    public static void writeSamplePdf(Path target) throws IOException {
        writePdf(target, SAMPLE_LINES);
    }

    public static void writePdf(Path target, List<String> lines) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.beginText();
                content.newLineAtOffset(50, 750);
                content.setLeading(16f);
                boolean first = true;
                for (String line : lines) {
                    if (first) {
                        content.showText(line);
                        first = false;
                    } else {
                        content.newLine();
                        content.showText(line);
                    }
                }
                content.endText();
            }

            document.save(target.toFile());
        }
    }

    public static void main(String[] args) throws IOException {
        Path target = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("src/test/resources/" + RESOURCE_PATH);
        writeSamplePdf(target);
        System.out.println("Fixture gerada em " + target.toAbsolutePath());
    }
}
