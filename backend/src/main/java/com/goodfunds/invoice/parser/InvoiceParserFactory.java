package com.goodfunds.invoice.parser;

import com.goodfunds.domain.Invoice;
import com.goodfunds.domain.OrigemFatura;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class InvoiceParserFactory {

    private final Map<OrigemFatura, InvoiceParser> parsersPorOrigem;

    public InvoiceParserFactory(List<InvoiceParser> parsers) {
        Map<OrigemFatura, InvoiceParser> map = new EnumMap<>(OrigemFatura.class);
        for (InvoiceParser parser : parsers) {
            InvoiceParser previous = map.put(parser.origem(), parser);
            if (previous != null) {
                throw new IllegalStateException(
                        "Mais de um InvoiceParser registrado para origem " + parser.origem()
                                + ": " + previous.getClass().getName()
                                + " e " + parser.getClass().getName()
                );
            }
        }
        this.parsersPorOrigem = Map.copyOf(map);
    }

    /**
     * Origens que possuem um {@link InvoiceParser} registrado e, portanto, podem
     * ser processadas. Usado para rejeitar o upload de origens sem parser antes
     * de salvar arquivo/registro.
     */
    public Set<OrigemFatura> origensSuportadas() {
        return parsersPorOrigem.keySet();
    }

    /**
     * Indica se a origem informada possui um parser registrado.
     */
    public boolean suporta(OrigemFatura origem) {
        return origem != null && parsersPorOrigem.containsKey(origem);
    }

    public InvoiceParser forInvoice(Invoice invoice) {
        Objects.requireNonNull(invoice, "invoice");
        return forOrigem(invoice.getOrigem());
    }

    public InvoiceParser forOrigem(OrigemFatura origem) {
        Objects.requireNonNull(origem, "origem");
        InvoiceParser parser = parsersPorOrigem.get(origem);
        if (parser == null) {
            throw new InvoiceParseException("Nenhum parser disponivel para origem " + origem);
        }
        return parser;
    }
}
