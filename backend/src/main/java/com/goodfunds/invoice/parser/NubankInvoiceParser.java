package com.goodfunds.invoice.parser;

import com.goodfunds.domain.OrigemFatura;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NubankInvoiceParser extends InvoiceParserSupport {

    private static final Map<String, Integer> MESES_PT = Map.ofEntries(
            Map.entry("JAN", 1),
            Map.entry("FEV", 2),
            Map.entry("MAR", 3),
            Map.entry("ABR", 4),
            Map.entry("MAI", 5),
            Map.entry("JUN", 6),
            Map.entry("JUL", 7),
            Map.entry("AGO", 8),
            Map.entry("SET", 9),
            Map.entry("OUT", 10),
            Map.entry("NOV", 11),
            Map.entry("DEZ", 12)
    );

    private static final Pattern MES_REFERENCIA = Pattern.compile(
            "(?im)m[eê]s\\s+de\\s+refer[eê]ncia\\s*[:\\-]?\\s*([A-Z]{3,})\\s*(?:de\\s+)?(\\d{4})"
    );

    private static final Pattern TOTAL_FATURA = Pattern.compile(
            "(?im)(?:valor\\s+total|total\\s+da\\s+fatura)\\s*[:\\-]?\\s*R\\$\\s*(-?[\\d.]+,\\d{2})"
    );

    private static final Pattern LANCAMENTO = Pattern.compile(
            "^(\\d{1,2})\\s+([A-Z]{3,})\\s+(.+)\\s+R\\$\\s*(-?[\\d.]+,\\d{2})\\s*$"
    );

    @Override
    public OrigemFatura origem() {
        return OrigemFatura.NUBANK;
    }

    @Override
    public ParsedInvoice parse(File pdf) {
        if (pdf == null) {
            throw new InvoiceParseException("Arquivo da fatura e obrigatorio");
        }
        if (!pdf.isFile() || !pdf.canRead()) {
            throw new InvoiceParseException("Arquivo da fatura inacessivel: " + pdf.getName());
        }

        String text;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            text = stripper.getText(document);
        } catch (IOException ex) {
            throw new InvoiceParseException("Falha ao ler PDF da fatura Nubank: " + pdf.getName(), ex);
        }

        return parseText(text);
    }

    ParsedInvoice parseText(String text) {
        YearMonth mesReferencia = extractMesReferencia(text);
        BigDecimal total = extractTotal(text);
        List<ParsedInvoiceTransaction> transacoes = extractTransacoes(text, mesReferencia);
        if (transacoes.isEmpty()) {
            throw new InvoiceParseException("Nenhum lancamento encontrado na fatura Nubank");
        }
        return new ParsedInvoice(mesReferencia, total, transacoes);
    }

    private YearMonth extractMesReferencia(String text) {
        Matcher matcher = MES_REFERENCIA.matcher(text);
        if (!matcher.find()) {
            throw new InvoiceParseException("Mes de referencia nao encontrado na fatura Nubank");
        }
        String monthToken = stripAccents(matcher.group(1)).toUpperCase(Locale.ROOT);
        int year = Integer.parseInt(matcher.group(2));
        Integer month = resolveMonth(monthToken);
        if (month == null) {
            throw new InvoiceParseException("Mes de referencia invalido: " + matcher.group(1));
        }
        return YearMonth.of(year, month);
    }

    private BigDecimal extractTotal(String text) {
        Matcher matcher = TOTAL_FATURA.matcher(text);
        if (!matcher.find()) {
            throw new InvoiceParseException("Total da fatura nao encontrado na fatura Nubank");
        }
        return parseValor(matcher.group(1));
    }

    private List<ParsedInvoiceTransaction> extractTransacoes(String text, YearMonth mesReferencia) {
        List<ParsedInvoiceTransaction> result = new ArrayList<>();
        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = LANCAMENTO.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            int day = Integer.parseInt(matcher.group(1));
            String monthToken = stripAccents(matcher.group(2)).toUpperCase(Locale.ROOT);
            Integer month = resolveMonth(monthToken);
            if (month == null) {
                continue;
            }
            String descricao = matcher.group(3).strip();
            BigDecimal valor = parseValor(matcher.group(4));
            LocalDate data = resolveDate(mesReferencia, month, day);
            result.add(new ParsedInvoiceTransaction(data, descricao, valor));
        }
        return result;
    }

    private Integer resolveMonth(String token) {
        return MESES_PT.get(token.substring(0, 3));
    }

    private String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
