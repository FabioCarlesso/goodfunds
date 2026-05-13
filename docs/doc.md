# Goodfunds — Documentação Geral do Sistema

## Visão do produto

O **Goodfunds** é um sistema de controle financeiro pessoal desenvolvido para uso próprio. Seu objetivo é oferecer uma visão clara e centralizada das finanças pessoais, cobrindo desde o registro de transações até a análise de faturas, planejamento por orçamentos e geração de relatórios com estimativas.

---

## Objetivo da aplicação

Permitir que o usuário:

- Registre e categorize suas receitas e despesas.
- Importe faturas de cartão de crédito (PDF) e processe-as automaticamente.
- Planeje seus gastos mensais por categoria com limites de orçamento.
- Acompanhe a evolução financeira ao longo do tempo com relatórios e projeções.

---

## Contexto funcional

O sistema opera com um único usuário autenticado por sessão (uso pessoal). Toda informação é isolada por usuário. A autenticação é feita via JWT com expiração de 24 horas.

Os dados financeiros são organizados em torno de cinco conceitos centrais:

| Conceito | Responsabilidade |
|---|---|
| **User** | Usuário autenticado; dono de todos os registros |
| **Category** | Classificação de receitas e despesas (ex: Alimentação, Salário) |
| **Transaction** | Lançamento financeiro manual ou gerado por uma fatura |
| **Invoice** | Fatura de cartão (PDF importado, processado automaticamente) |
| **Budget** | Limite de gasto mensal por categoria |

---

## Arquitetura do sistema

```
┌─────────────────────────────────────────────────────────┐
│                        FRONTEND                         │
│         Vite + React + TypeScript + Tailwind            │
│                                                         │
│  Dashboard │ Faturas │ Planejamento │ Relatórios │ Config│
└─────────────────────────┬───────────────────────────────┘
                          │ REST / JSON + JWT
┌─────────────────────────▼───────────────────────────────┐
│                        BACKEND                          │
│              Java 17 + Spring Boot 3 + Maven            │
│                                                         │
│  Auth API  │ Fatura API │ Finance API │ Report API │ Docs│
│  (JWT)     │ (PDF/Parse)│ (Transações)│ (Projeções)│ (SW)│
└─────────────────────────┬───────────────────────────────┘
                          │ JPA / Hibernate (Flyway)
┌─────────────────────────▼───────────────────────────────┐
│                     BANCO DE DADOS                      │
│         PostgreSQL (prod) · H2 in-memory (testes)       │
│                                                         │
│  users │ categories │ transactions │ invoices │ budgets  │
└─────────────────────────────────────────────────────────┘
```

---

## Principais módulos e responsabilidades

| Módulo | Camada | Responsabilidade |
|---|---|---|
| Auth | Backend | Registro, login e geração de JWT |
| Categories | Backend | CRUD de categorias por usuário; seed automático no registro |
| Transactions | Backend | CRUD de lançamentos financeiros com paginação e filtros |
| Invoices | Backend | Upload de PDF, parse automático e geração de transações |
| Budgets | Backend | Limites mensais por categoria com constraint única |
| Reports | Backend | Resumo mensal, gastos por categoria, evolução e estimativas |
| Security | Backend | Filtro JWT, Spring Security, controle de acesso por usuário |
| Frontend | Frontend | Interface web consumindo a API REST |

---

## Fluxos principais

### Fluxo de autenticação

```
Usuário → POST /auth/register → Cria User + seed de categorias padrão → JWT
Usuário → POST /auth/login    → Valida credenciais → JWT (24h)
Todas as demais rotas exigem: Authorization: Bearer <token>
```

### Fluxo de lançamento manual

```
Usuário seleciona categoria → preenche valor, data e descrição
→ POST /transactions
→ Transação registrada com tipo herdado da categoria (RECEITA/DESPESA)
```

### Fluxo de importação de fatura

```
Usuário faz upload do PDF (Nubank)
→ POST /invoices/upload (multipart)
→ PDF salvo em ./uploads/{userId}/
→ NubankInvoiceParser extrai lançamentos com PDFBox
→ Transações geradas e vinculadas à Invoice
→ Usuário pode ajustar categorias manualmente
```

### Fluxo de planejamento

```
Usuário define limite por categoria e mês
→ POST /budgets
→ Sistema compara limite com gasto real nas consultas de relatório
→ GET /reports/summary?ref=YYYY-MM retorna receitas, despesas, orçado e saldo
```

---

## Regras de negócio

- O tipo de uma transação (RECEITA/DESPESA) é determinado pela categoria associada, não pelo valor.
- O valor de uma transação é sempre positivo.
- Cada usuário pode ter apenas um orçamento por categoria por mês (constraint UNIQUE).
- O seed de categorias padrão é criado automaticamente no registro: Alimentação, Transporte, Moradia, Lazer, Saúde, Educação, Salário, Outros.
- No MVP, apenas faturas do Nubank são suportadas pelo parser.
- PDFs de faturas são armazenados no filesystem local (`./uploads/{userId}/`), não no banco de dados.
- JWT expira em 24 horas; não há refresh token no MVP.

---

## Stack do sistema

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Build | Maven (Maven Wrapper) |
| Segurança | Spring Security + JJWT 0.12.6 |
| Persistência | JPA / Hibernate |
| Migrations | Flyway |
| Banco (dev/prod) | PostgreSQL |
| Banco (testes) | H2 in-memory |
| Documentação de API | Springdoc OpenAPI 3 / Swagger UI |
| Cache | Caffeine |
| Observabilidade | Spring Boot Actuator |
| Validação | Bean Validation (jakarta.validation) |
| Parser de PDF | Apache PDFBox 3.0.3 |
| Testes | JUnit 5 + Spring Security Test |
| Frontend | Vite + React + TypeScript + Tailwind |
| Containerização | Docker Compose (planejado) |

---

## Estrutura do repositório

```text
goodfunds/
├── backend/          # Projeto Spring Boot (Maven)
│   ├── src/
│   └── pom.xml
├── docs/             # Documentação técnica e planejamento
│   ├── doc.md
│   ├── doc-backend.md
│   ├── doc-frontend.md
│   ├── index.html
│   └── goodfunds-planejamento.md
├── frontend/         # Projeto Vite + React (Sprint 4)
└── README.md
```

---

## Roadmap do MVP

| Sprint | Período | Objetivo | Status |
|---|---|---|---|
| Sprint 1 | Semanas 1–2 | Fundação: auth + schema Flyway | Em andamento |
| Sprint 2 | Semanas 3–4 | Core financeiro: transações + faturas | Pendente |
| Sprint 3 | Semanas 5–6 | Planejamento e relatórios | Pendente |
| Sprint 4 | Semanas 7–8 | Frontend + Docker + finalização | Pendente |

---

## Estado atual

- Backend inicializado com Spring Boot 3.3.4 em `backend/`.
- Estrutura de pacotes `com.goodfunds` criada com `package-info.java` em cada pacote.
- Perfis Spring configurados: `dev` (PostgreSQL local), `test` (H2), `prod` (variáveis de ambiente).
- Flyway incluído no classpath; habilitado apenas em `prod` até a criação da primeira migration.
- Smoke test de contexto Spring passando no perfil `test`.
- Frontend, Docker Compose e migrations ainda não implementados.
