ALTER TABLE invoices
    ALTER COLUMN mes_referencia DROP NOT NULL;

ALTER TABLE invoices
    ALTER COLUMN total_valor DROP NOT NULL;
