CREATE TABLE users (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    nome       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    senha      VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER',
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

CREATE TABLE categories (
    id      UUID         NOT NULL DEFAULT gen_random_uuid(),
    nome    VARCHAR(255) NOT NULL,
    tipo    VARCHAR(20)  NOT NULL,
    user_id UUID         NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
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
    created_at     TIMESTAMP WITH TIME ZONE   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT fk_invoices_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
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
    created_at      TIMESTAMP WITH TIME ZONE   NOT NULL DEFAULT NOW(),
    -- updated_at requer @UpdateTimestamp na entidade JPA para ser atualizado automaticamente
    updated_at      TIMESTAMP WITH TIME ZONE   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT ck_transactions_valor CHECK (valor > 0),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_invoice  FOREIGN KEY (invoice_id)  REFERENCES invoices (id)  ON DELETE SET NULL,
    CONSTRAINT fk_transactions_user     FOREIGN KEY (user_id)     REFERENCES users (id)     ON DELETE CASCADE
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
    CONSTRAINT ck_budgets_limite CHECK (limite > 0),
    CONSTRAINT ck_budgets_mes   CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT ck_budgets_ano   CHECK (ano >= 2000),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_budgets_user     FOREIGN KEY (user_id)     REFERENCES users (id)     ON DELETE CASCADE,
    CONSTRAINT uq_budgets_user_category_mes_ano UNIQUE (user_id, category_id, mes, ano)
);

CREATE INDEX idx_budgets_category_id ON budgets (category_id);
