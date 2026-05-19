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
 * Gera a fixture PDF da fatura Nubank usada nos testes.
 *
 * <p>O metodo {@link #main(String[])} grava a fixture em
 * {@code src/test/resources/invoices/nubank/sample-fatura.pdf}, permitindo
 * regenerar o arquivo deterministicamente sempre que o formato esperado pelo
 * parser mudar.</p>
 */
public final class NubankInvoiceFixtures {

    public static final String RESOURCE_PATH = "invoices/nubank/sample-fatura.pdf";

    public static final List<String> SAMPLE_LINES = List.of(
            "Fatura Nubank",
            "Mes de referencia: JUN de 2025",
            "Vencimento: 15 JUL 2025",
            "Valor total: R$ 1.234,56",
            "",
            "Transacoes",
            "01 JUN MERCADO LIVRE R$ 89,90",
            "05 JUN UBER TRIP R$ 35,40",
            "12 JUN PADARIA CENTRAL R$ 24,80",
            "28 MAI NETFLIX R$ 55,90",
            "30 MAI POSTO SHELL R$ 200,00"
    );

    private NubankInvoiceFixtures() {
    }

    public static void writeSamplePdf(Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.beginText();
                content.newLineAtOffset(50, 750);
                content.setLeading(16f);
                boolean first = true;
                for (String line : SAMPLE_LINES) {
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
