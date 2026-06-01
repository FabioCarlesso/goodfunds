package com.goodfunds.invoice.parser;

import com.goodfunds.domain.OrigemFatura;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de faturas do Itau.
 *
 * <p>Diferente do {@link NubankInvoiceParser}, a fatura do Itau usa datas no
 * formato numerico {@code DD/MM}, mes de referencia {@code MM/AAAA} e valores no
 * padrao brasileiro sem prefixo {@code R$} em cada lancamento.</p>
 */
@Component
public class ItauInvoiceParser implements InvoiceParser {

    private static final Pattern MES_REFERENCIA = Pattern.compile(
            "(?im)m[eê]s\\s+de\\s+refer[eê]ncia\\s*[:\\-]?\\s*(\\d{1,2})/(\\d{4})"
    );

    private static final Pattern TOTAL_FATURA = Pattern.compile(
            "(?im)total\\s+(?:desta|da)\\s+fatura\\s*[:\\-]?\\s*R\\$\\s*(-?[\\d.]+,\\d{2})"
    );

    private static final Pattern LANCAMENTO = Pattern.compile(
            "^(\\d{1,2})/(\\d{1,2})\\s+(.+?)\\s+(-?[\\d.]+,\\d{2})$"
    );

    @Override
    public OrigemFatura origem() {
        return OrigemFatura.ITAU;
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
            throw new InvoiceParseException("Falha ao ler PDF da fatura Itau: " + pdf.getName(), ex);
        }

        return parseText(text);
    }

    ParsedInvoice parseText(String text) {
        YearMonth mesReferencia = extractMesReferencia(text);
        BigDecimal total = extractTotal(text);
        List<ParsedInvoiceTransaction> transacoes = extractTransacoes(text, mesReferencia);
        if (transacoes.isEmpty()) {
            throw new InvoiceParseException("Nenhum lancamento encontrado na fatura Itau");
        }
        return new ParsedInvoice(mesReferencia, total, transacoes);
    }

    private YearMonth extractMesReferencia(String text) {
        Matcher matcher = MES_REFERENCIA.matcher(text);
        if (!matcher.find()) {
            throw new InvoiceParseException("Mes de referencia nao encontrado na fatura Itau");
        }
        int month = Integer.parseInt(matcher.group(1));
        int year = Integer.parseInt(matcher.group(2));
        if (month < 1 || month > 12) {
            throw new InvoiceParseException("Mes de referencia invalido: " + matcher.group(1));
        }
        return YearMonth.of(year, month);
    }

    private BigDecimal extractTotal(String text) {
        Matcher matcher = TOTAL_FATURA.matcher(text);
        if (!matcher.find()) {
            throw new InvoiceParseException("Total da fatura nao encontrado na fatura Itau");
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
            int month = Integer.parseInt(matcher.group(2));
            if (month < 1 || month > 12) {
                continue;
            }
            String descricao = matcher.group(3).strip();
            BigDecimal valor = parseValor(matcher.group(4));
            LocalDate data = resolveDate(mesReferencia, month, day);
            result.add(new ParsedInvoiceTransaction(data, descricao, valor));
        }
        return result;
    }

    private LocalDate resolveDate(YearMonth mesReferencia, int month, int day) {
        int year = mesReferencia.getYear();
        if (month > mesReferencia.getMonthValue()) {
            year -= 1;
        }
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException ex) {
            throw new InvoiceParseException(
                    "Data invalida na fatura Itau: " + day + "/" + month + "/" + year, ex);
        }
    }

    private BigDecimal parseValor(String raw) {
        String normalized = raw.replace(".", "").replace(',', '.');
        return new BigDecimal(normalized);
    }
}
