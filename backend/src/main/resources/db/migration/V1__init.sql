CREATE TABLE users (
    id         UUID          NOT NULL DEFAULT gen_random_uuid(),
    nome       VARCHAR(255)  NOT NULL,
    email      VARCHAR(255)  NOT NULL,
    senha      VARCHAR(255)  NOT NULL,
    role       VARCHAR(50)   NOT NULL DEFAULT 'ROLE_USER',
    enabled    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id      UUID         NOT NULL DEFAULT gen_random_uuid(),
    nome    VARCHAR(255) NOT NULL,
    tipo    VARCHAR(20)  NOT NULL,
    user_id UUID         NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_categories_user_id ON categories (user_id);

CREATE TABLE invoices (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    arquivo        VARCHAR(500)  NOT NULL,
    origem         VARCHAR(20)   NOT NULL,
    status         VARCHAR(30)   NOT NULL,
    mes_referencia VARCHAR(7)    NOT NULL,
    total_valor    NUMERIC(19,2) NOT NULL,
    user_id        UUID          NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT fk_invoices_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_invoices_user_id ON invoices (user_id);

CREATE TABLE transactions (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    descricao       VARCHAR(500)  NOT NULL,
    valor           NUMERIC(19,2) NOT NULL,
    data            DATE          NOT NULL,
    forma_pagamento VARCHAR(30)   NOT NULL,
    category_id     UUID          NOT NULL,
    invoice_id      UUID,
    user_id         UUID          NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_transactions_invoice  FOREIGN KEY (invoice_id)  REFERENCES invoices (id),
    CONSTRAINT fk_transactions_user     FOREIGN KEY (user_id)     REFERENCES users (id)
);

CREATE INDEX idx_transactions_user_id     ON transactions (user_id);
CREATE INDEX idx_transactions_category_id ON transactions (category_id);
CREATE INDEX idx_transactions_invoice_id  ON transactions (invoice_id);
CREATE INDEX idx_transactions_data        ON transactions (data);

CREATE TABLE budgets (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    limite      NUMERIC(19,2) NOT NULL,
    category_id UUID          NOT NULL,
    mes         INTEGER       NOT NULL,
    ano         INTEGER       NOT NULL,
    user_id     UUID          NOT NULL,
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_budgets_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT uq_budgets_user_category_mes_ano UNIQUE (user_id, category_id, mes, ano)
);

CREATE INDEX idx_budgets_user_id     ON budgets (user_id);
CREATE INDEX idx_budgets_category_id ON budgets (category_id);
