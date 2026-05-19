package com.goodfunds.invoice.parser;

import com.goodfunds.domain.OrigemFatura;

import java.io.File;

public interface InvoiceParser {

    OrigemFatura origem();

    ParsedInvoice parse(File pdf);
}
