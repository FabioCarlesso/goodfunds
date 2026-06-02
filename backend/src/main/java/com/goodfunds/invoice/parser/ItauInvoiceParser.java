package com.goodfunds.invoice.parser;

import com.goodfunds.domain.OrigemFatura;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de faturas do Itau.
 *
 * <p>A fatura real do Itau usa um leiaute de <strong>duas colunas</strong>: cada
 * pagina lista lancamentos lado a lado, e o {@link PDFTextStripper} (com ordenacao
 * por posicao) mescla as duas colunas numa mesma linha fisica. Por isso a extracao
 * e feita por regiao (coluna esquerda/direita), separadas por um <em>gutter</em>
 * (o vao vertical entre as colunas) detectado dinamicamente.</p>
 *
 * <p>O mes de referencia e derivado do {@code Vencimento: DD/MM/AAAA} (a fatura nao
 * traz "mes de referencia"); o total vem de {@code Total desta fatura}. So contam os
 * lancamentos entre {@code Lancamentos: compras e saques} e
 * {@code Total dos lancamentos atuais}: a tabela {@code Compras parceladas - proximas
 * faturas} (projecoes de faturas futuras) e excluida, assim como pagamentos/estornos
 * (valores negativos sao ignorados).</p>
 */
@Component
public class ItauInvoiceParser extends InvoiceParserSupport {

    private static final Pattern VENCIMENTO = Pattern.compile(
            "(?im)vencimento\\s*[:\\-]?\\s*(\\d{1,2})/(\\d{1,2})/(\\d{4})"
    );

    private static final Pattern TOTAL_FATURA = Pattern.compile(
            "(?im)total\\s+desta\\s+fatura\\s*[:\\-]?\\s*R?\\$?\\s*(-?[\\d.]+,\\d{2})"
    );

    /** Inicio da secao de lancamentos atuais. */
    private static final Pattern INICIO_LANCAMENTOS = Pattern.compile(
            "(?i)lan.amentos:\\s*compras\\s+e\\s+saques"
    );

    /** Fim dos lancamentos atuais; tudo a partir daqui (projecoes) e descartado. */
    private static final Pattern FIM_LANCAMENTOS = Pattern.compile(
            "(?i)total\\s+dos\\s+lan.amentos\\s+atuais"
                    + "|compras\\s+parceladas\\s*[-–]\\s*pr.ximas"
    );

    private static final Pattern LANCAMENTO = Pattern.compile(
            "(\\d{1,2})/(\\d{1,2})\\s+(.+?)\\s+(-?[\\d.]+,\\d{2})"
    );

    /** Linha que contem (ao menos) dois lancamentos: usada para detectar o gutter. */
    private static final Pattern LINHA_DUAS_COLUNAS = Pattern.compile(
            "\\d{1,2}/\\d{1,2}\\s+.+?\\s+-?[\\d.]+,\\d{2}.*\\d{1,2}/\\d{1,2}\\s+.+?\\s+-?[\\d.]+,\\d{2}"
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

        try (PDDocument document = Loader.loadPDF(pdf)) {
            return parseDocument(document);
        } catch (IOException ex) {
            throw new InvoiceParseException("Falha ao ler PDF da fatura Itau: " + pdf.getName(), ex);
        }
    }

    ParsedInvoice parseDocument(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        String fullText = stripper.getText(document);

        YearMonth mesReferencia = extractMesReferencia(fullText);
        BigDecimal total = extractTotal(fullText);
        List<ParsedInvoiceTransaction> transacoes = extractTransacoes(document, mesReferencia);
        if (transacoes.isEmpty()) {
            throw new InvoiceParseException("Nenhum lancamento encontrado na fatura Itau");
        }
        return new ParsedInvoice(mesReferencia, total, transacoes);
    }

    private YearMonth extractMesReferencia(String text) {
        Matcher matcher = VENCIMENTO.matcher(text);
        if (!matcher.find()) {
            throw new InvoiceParseException(
                    "Vencimento (mes de referencia) nao encontrado na fatura Itau");
        }
        int month = Integer.parseInt(matcher.group(2));
        int year = Integer.parseInt(matcher.group(3));
        if (month < 1 || month > 12) {
            throw new InvoiceParseException("Mes de vencimento invalido: " + matcher.group(2));
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

    /**
     * Extrai os lancamentos respeitando o leiaute de duas colunas. Percorre cada
     * pagina na ordem de leitura (coluna esquerda e depois direita), iniciando na
     * secao de compras e parando ao encontrar o total dos lancamentos atuais (o que
     * descarta a tabela de proximas faturas). Valores negativos (pagamentos/estornos)
     * sao ignorados.
     */
    private List<ParsedInvoiceTransaction> extractTransacoes(
            PDDocument document, YearMonth mesReferencia) throws IOException {

        float gutter = detectGutter(document);
        List<ParsedInvoiceTransaction> result = new ArrayList<>();
        boolean started = false;

        for (PDPage page : document.getPages()) {
            PDRectangle box = page.getMediaBox();
            float width = box.getWidth();
            float height = box.getHeight();

            PDFTextStripperByArea area = new PDFTextStripperByArea();
            area.setSortByPosition(true);
            area.addRegion("L", new Rectangle2D.Float(0, 0, gutter, height));
            area.addRegion("R", new Rectangle2D.Float(gutter, 0, width - gutter, height));
            area.extractRegions(page);

            for (String region : new String[] {"L", "R"}) {
                for (String rawLine : area.getTextForRegion(region).split("\\r?\\n")) {
                    String line = rawLine.strip();
                    if (line.isEmpty()) {
                        continue;
                    }
                    if (!started) {
                        if (INICIO_LANCAMENTOS.matcher(line).find()) {
                            started = true;
                        }
                        continue;
                    }
                    if (FIM_LANCAMENTOS.matcher(line).find()) {
                        return result;
                    }
                    collectLine(line, mesReferencia, result);
                }
            }
        }
        return result;
    }

    private void collectLine(String line, YearMonth mesReferencia,
                             List<ParsedInvoiceTransaction> result) {
        Matcher matcher = LANCAMENTO.matcher(line);
        while (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            if (month < 1 || month > 12) {
                continue;
            }
            BigDecimal valor = parseValor(matcher.group(4));
            if (valor.signum() <= 0) {
                // Pagamentos/estornos: ignorados no parser do Itau (ver issue de
                // tratamento global de valores negativos).
                continue;
            }
            String descricao = matcher.group(3).strip();
            LocalDate data = resolveDate(mesReferencia, month, day);
            result.add(new ParsedInvoiceTransaction(data, descricao, valor));
        }
    }

    /**
     * Detecta o gutter (coordenada X que separa as duas colunas) a partir das linhas
     * que contem dois lancamentos: marca a ocupacao horizontal dos glifos dessas
     * linhas e escolhe a maior faixa vazia na regiao central da pagina.
     */
    private float detectGutter(PDDocument document) throws IOException {
        float width = document.getPage(0).getMediaBox().getWidth();
        GutterFinder finder = new GutterFinder(width);
        finder.setSortByPosition(true);
        finder.getText(document);
        return finder.gutterX();
    }

    /** Coleta a ocupacao horizontal das linhas de duas colunas para achar o gutter. */
    private static final class GutterFinder extends PDFTextStripper {

        private final boolean[] occupied;
        private final float width;

        private GutterFinder(float width) throws IOException {
            this.width = width;
            this.occupied = new boolean[(int) Math.ceil(width) + 2];
        }

        @Override
        protected void writeString(String text, List<TextPosition> positions) {
            if (!LINHA_DUAS_COLUNAS.matcher(text).find()) {
                return;
            }
            for (TextPosition p : positions) {
                int from = (int) Math.floor(p.getXDirAdj());
                int to = (int) Math.ceil(p.getXDirAdj() + p.getWidthDirAdj());
                for (int x = Math.max(0, from); x < Math.min(occupied.length, to); x++) {
                    occupied[x] = true;
                }
            }
        }

        private float gutterX() {
            int lo = (int) (width * 0.54f);
            int hi = (int) (width * 0.62f);
            int bestStart = -1;
            int bestLen = 0;
            int run = -1;
            for (int x = lo; x < hi; x++) {
                if (!occupied[x]) {
                    if (run < 0) {
                        run = x;
                    }
                } else if (run >= 0) {
                    if (x - run > bestLen) {
                        bestLen = x - run;
                        bestStart = run;
                    }
                    run = -1;
                }
            }
            if (run >= 0 && hi - run > bestLen) {
                bestLen = hi - run;
                bestStart = run;
            }
            return bestStart < 0 ? width * 0.58f : bestStart + bestLen / 2f;
        }
    }
}
