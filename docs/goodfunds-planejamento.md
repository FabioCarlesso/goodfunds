# Goodfunds — Planejamento do Sistema

> Sistema de controle financeiro pessoal · Uso pessoal · MVP end-to-end

---

## Visão geral

O **Goodfunds** é um sistema de controle financeiro pessoal com foco em quatro pilares:

- Avaliação de faturas
- Planejamento financeiro
- Estimativas e dashboards
- Gestão de transações e categorias

---

## Stack definida

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3 |
| Build | Maven |
| Segurança | Spring Security + JWT (24h, sem refresh no MVP) |
| Persistência | JPA / Hibernate |
| Migrations | Flyway |
| Banco (prod) | PostgreSQL |
| Banco (testes) | H2 in-memory |
| Documentação | Swagger / OpenAPI 3 |
| Cache | Caffeine |
| Observabilidade | Spring Boot Actuator |
| Validação | Bean Validation (jakarta.validation) |
| Parser de PDF | Apache PDFBox |
| Testes | JUnit 5 |
| Containerização | Docker Compose |
| Frontend MVP | Vite + React + TypeScript + Tailwind |
| IDE | IntelliJ IDEA |
| Versionamento | Git |

---

## Arquitetura

### Camadas do sistema

```
┌─────────────────────────────────────────────────────────┐
│                        FRONTEND                         │
│         Vite + React + TypeScript + Tailwind            │
│                                                         │
│  Dashboard │ Faturas │ Planejamento │ Relatórios │ Config│
└─────────────────────────┬───────────────────────────────┘
                          │ REST / JSON
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
│  User │ Invoice │ Transaction │ Category │ Budget        │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                    INFRA & TOOLING                      │
│  Docker │ Git │ JUnit 5 │ Caffeine │ Actuator │ IntelliJ │
└─────────────────────────────────────────────────────────┘
```

### Estrutura de pacotes

```
com.goodfunds
├── controller      # REST controllers
├── service         # Regras de negócio
├── repository      # Interfaces JPA
├── domain          # Entidades JPA
├── dto             # Data Transfer Objects
├── config          # Configurações gerais
├── security        # JWT, filtros, Spring Security
└── exception       # GlobalExceptionHandler e exceções de domínio
```

### Layout do repositório

```
goodfunds/
├── backend/                  # Projeto Spring Boot (Maven)
├── frontend/                 # Projeto Vite + React + TS (Sprint 4)
├── docker/                   # Dockerfiles e compose
├── docs/                     # Planejamento e documentação
└── docker-compose.yml
```

---

## Entidades principais

### User
| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador |
| nome | String | Nome do usuário |
| email | String | E-mail (único) |
| senha | String | Hash bcrypt |
| role | Enum | `ROLE_USER` (default) |
| enabled | boolean | Habilitado (default `true`) |
| createdAt | LocalDateTime | Data de criação |

### Category
| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador |
| nome | String | Nome da categoria |
| tipo | Enum | `RECEITA` ou `DESPESA` |
| user | User | Dono da categoria |

> Seed automático no registro de usuário com categorias padrão: Alimentação, Transporte, Moradia, Lazer, Saúde, Educação, Salário, Outros.

### Transaction
| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador |
| descricao | String | Descrição do lançamento |
| valor | BigDecimal | Valor (sempre positivo; o tipo vem da categoria) |
| data | LocalDate | Data da transação |
| formaPagamento | Enum | `CARTAO_CREDITO`, `CARTAO_DEBITO`, `PIX`, `DINHEIRO`, `BOLETO`, `TRANSFERENCIA` |
| category | Category | Categoria associada (determina RECEITA/DESPESA) |
| invoice | Invoice | Opcional (`null` quando lançada manualmente) |
| user | User | Dono da transação |
| createdAt | LocalDateTime | Auditoria |
| updatedAt | LocalDateTime | Auditoria |

### Invoice
| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador |
| arquivo | String | Path relativo do PDF salvo no diretório de uploads |
| origem | Enum | `NUBANK`, `ITAU`, `OUTROS` (MVP suporta apenas `NUBANK`) |
| status | Enum | `PENDENTE_PARSE`, `PROCESSADA`, `ERRO` |
| mesReferencia | YearMonth | Mês da fatura |
| totalValor | BigDecimal | Valor total |
| transactions | List<Transaction> | Lançamentos gerados pelo parser |
| user | User | Dono da fatura |
| createdAt | LocalDateTime | Auditoria |

> Armazenamento dos PDFs: filesystem local em `./uploads/{userId}/...`. Não persistir bytes no DB.

### Budget
| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador |
| limite | BigDecimal | Limite planejado |
| category | Category | Categoria do orçamento |
| mes | Integer | Mês (1–12) |
| ano | Integer | Ano |
| user | User | Dono do orçamento |

> Constraint **UNIQUE(user_id, category_id, mes, ano)** no banco.

---

## Decisões transversais

- **Migrations:** Flyway desde a Sprint 1. `spring.jpa.hibernate.ddl-auto=validate` em prod, `update` apenas em dev local se necessário.
- **JWT:** expiração de 24h, sem refresh token no MVP. Segredo em `application.yml` (perfil `prod` lê de variável de ambiente).
- **Senhas:** BCrypt.
- **Erros:** `@RestControllerAdvice` (`GlobalExceptionHandler`) traduzindo exceções para `ProblemDetail` (RFC 7807).
- **Validação:** `@Valid` em todos os payloads, com `@NotBlank`, `@Positive`, `@Email`, etc. nos DTOs.
- **Paginação:** todos os endpoints de listagem retornam `Page<T>` (`page`, `size`, `sort`).
- **Cache Caffeine:** aplicado em `/reports/*`. Invalidação automática ao criar/editar/remover `Transaction` ou `Budget`.
- **Actuator:** expor `/actuator/health` e `/actuator/info`. Demais endpoints só com autenticação.
- **Parser de PDF (MVP):** apenas faturas do **Nubank** com Apache PDFBox. Arquitetura preparada para adicionar novos parsers (interface `InvoiceParser` + estratégia por `origem`).

---

## Roadmap MVP — 8 semanas

### Sprint 1 — Fundação (Semanas 1–2)

**Objetivo:** API rodando localmente com auth funcional e schema versionado.

- [x] Criar projeto Spring Boot 3 com Maven em `backend/`
- [x] Definir estrutura de pacotes (`com.goodfunds`)
- [ ] Configurar `application.yml` com perfis `dev`, `test` e `prod`
- [ ] Configurar **Flyway** + `V1__init.sql` com as 5 tabelas
- [ ] Mapear entidades JPA: `User`, `Category`, `Transaction`, `Invoice`, `Budget`
- [ ] Implementar autenticação com JWT (login, registro, token 24h)
- [ ] Seed automático de categorias padrão no registro de usuário
- [ ] Configurar **Spring Boot Actuator** (`/actuator/health`)
- [ ] Configurar `GlobalExceptionHandler` (`@RestControllerAdvice` + `ProblemDetail`)
- [ ] Configurar banco H2 para testes e PostgreSQL para dev
- [ ] Docker Compose básico (app + postgres)

**Entregável:** `POST /auth/register` e `POST /auth/login` funcionando, schema gerenciado por Flyway.

---

### Sprint 2 — Core financeiro (Semanas 3–4)

**Objetivo:** Fatura registrada e categorizada.

- [ ] CRUD completo de `Transaction` com paginação e filtros (mes/ano/categoria/tipo/intervalo)
- [ ] CRUD de `Category`
- [ ] Endpoint de upload de fatura PDF (multipart, salvar em `./uploads/{userId}/`)
- [ ] Interface `InvoiceParser` + implementação **NubankInvoiceParser** com Apache PDFBox
- [ ] Geração automática de `Transaction` a partir da fatura processada
- [ ] Categorização manual de transações (atualizar `categoryId`)
- [ ] Sugestão automática de categoria por descrição (regra simples por palavras-chave)

**Entregável:** Fatura Nubank importada com transações geradas e categorizadas.

---

### Sprint 3 — Planejamento e relatórios (Semanas 5–6)

**Objetivo:** Dashboard com dados reais.

- [ ] CRUD de `Budget` (limite mensal por categoria, com unique constraint)
- [ ] Engine de estimativas (projeção do mês atual com base em média dos últimos 3 meses)
- [ ] API de relatórios: gastos por categoria, evolução mensal
- [ ] Endpoint de resumo mensal (receitas vs despesas vs orçado) com parâmetro `?ref=YYYY-MM`
- [ ] **Caffeine cache** nas consultas de relatório, com invalidação ao alterar `Transaction`/`Budget`

**Entregável:** Endpoints de dashboard prontos para consumo do frontend.

---

### Sprint 4 — Finalização MVP (Semanas 7–8)

**Objetivo:** Sistema end-to-end funcional.

- [ ] Scaffold frontend com **Vite + React + TypeScript + Tailwind** em `frontend/`
- [ ] Tela de Login/Registro + persistência do JWT
- [ ] Telas MVP: Dashboard, Faturas, Planejamento, Relatórios
- [ ] Swagger UI configurado e documentado (OpenAPI 3)
- [ ] Docker Compose completo (app + postgres + frontend)
- [ ] Testes JUnit 5 para todos os services
- [ ] README.md com instruções de setup e execução
- [ ] Revisão de segurança (validações, tratamento de erros global, headers)

**Entregável:** MVP rodando via `docker compose up`.

---

## Endpoints previstos (MVP)

### Auth
| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/register` | Cadastro de usuário (gera categorias padrão) |
| POST | `/auth/login` | Login e geração de JWT (24h) |

### Transactions
| Método | Rota | Descrição |
|---|---|---|
| GET | `/transactions` | Lista paginada (filtros: `?ref=YYYY-MM`, `categoryId`, `tipo`, `from`, `to`, `page`, `size`) |
| POST | `/transactions` | Cria transação |
| PUT | `/transactions/{id}` | Atualiza transação |
| DELETE | `/transactions/{id}` | Remove transação |

### Categories
| Método | Rota | Descrição |
|---|---|---|
| GET | `/categories` | Lista categorias |
| POST | `/categories` | Cria categoria |
| PUT | `/categories/{id}` | Atualiza categoria |
| DELETE | `/categories/{id}` | Remove categoria |

### Invoices
| Método | Rota | Descrição |
|---|---|---|
| POST | `/invoices/upload` | Upload de fatura PDF (multipart) |
| GET | `/invoices` | Lista faturas |
| GET | `/invoices/{id}` | Detalhe da fatura + transações geradas |

### Budgets
| Método | Rota | Descrição |
|---|---|---|
| GET | `/budgets?ref=YYYY-MM` | Lista orçamentos do mês |
| POST | `/budgets` | Cria orçamento |
| PUT | `/budgets/{id}` | Atualiza orçamento |

### Reports
| Método | Rota | Descrição |
|---|---|---|
| GET | `/reports/summary?ref=YYYY-MM` | Resumo mensal (receitas, despesas, orçado, saldo) |
| GET | `/reports/by-category?ref=YYYY-MM` | Gastos por categoria |
| GET | `/reports/evolution?from=YYYY-MM&to=YYYY-MM` | Evolução mensal |
| GET | `/reports/estimate` | Projeção do mês atual baseada nos últimos 3 meses |

### Actuator
| Método | Rota | Descrição |
|---|---|---|
| GET | `/actuator/health` | Health check público |
| GET | `/actuator/info` | Informações do build |

---

## CLAUDE.md (raiz do projeto)

```markdown
# Goodfunds — Personal Finance System

## Stack
- Java 17 + Spring Boot 3 + Maven (em backend/)
- PostgreSQL (prod), H2 (testes)
- Flyway para migrations
- Spring Security + JWT (24h)
- JPA / Hibernate
- Apache PDFBox (parser de faturas)
- Swagger / OpenAPI 3
- Docker Compose
- JUnit 5 + Caffeine Cache + Actuator
- Vite + React + TypeScript + Tailwind (em frontend/)

## Pacote base
com.goodfunds

## Convenções
- REST padrão com ResponseEntity e Page<T> em listagens
- DTOs separados das entidades + Bean Validation
- GlobalExceptionHandler com ProblemDetail (RFC 7807)
- Schema só via Flyway (ddl-auto=validate)
- Testes para todos os services
```

---

*Goodfunds · planejamento gerado em maio/2026 · revisão 2*
