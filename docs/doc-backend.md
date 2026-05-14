# Goodfunds — Documentação Técnica do Backend

## Visão geral

O backend do Goodfunds é uma API REST desenvolvida em **Java 17** com **Spring Boot 3.3.4**, construída com Maven e estruturada em camadas bem definidas. Ele é o núcleo do sistema: gerencia autenticação, lógica de negócio, persistência e processamento de faturas PDF.

---

## Arquitetura

O backend segue a arquitetura em camadas clássica do Spring Boot:

```
┌─────────────────────────────────────────────┐
│             Cliente (Frontend/API)           │
└──────────────────────┬──────────────────────┘
                       │ HTTP + JWT
┌──────────────────────▼──────────────────────┐
│  Camada de Segurança (Spring Security + JWT) │
│  JwtAuthFilter → valida token → contexto    │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│  Controller (REST)                          │
│  @RestController · ResponseEntity · @Valid  │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│  Service (Regras de negócio)                │
│  @Service · lógica de domínio · cache       │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│  Repository (Acesso a dados)                │
│  JpaRepository · @Query · paginação         │
└──────────────────────┬──────────────────────┘
                       │
┌──────────────────────▼──────────────────────┐
│  Banco de dados                             │
│  PostgreSQL (dev/prod) · H2 (testes)        │
│  Schema gerenciado por Flyway               │
└─────────────────────────────────────────────┘
```

---

## Estrutura de pacotes

```
com.goodfunds
├── GoodfundsApplication.java   # Ponto de entrada Spring Boot
├── controller/                 # REST controllers da API
├── service/                    # Regras de negócio
├── repository/                 # Interfaces Spring Data JPA
├── domain/                     # Entidades JPA e enums de domínio
├── dto/                        # Contratos de entrada/saída (Bean Validation)
├── config/                     # Configurações transversais (cache, OpenAPI)
├── security/                   # Spring Security, filtro JWT
└── exception/                  # GlobalExceptionHandler + exceções de domínio
```

Cada pacote possui um `package-info.java` documentando seu papel.

---

## Entidades JPA

### User

| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador único |
| nome | String | Nome do usuário |
| email | String | E-mail (único) |
| senha | String | Hash BCrypt |
| role | Enum | `ROLE_USER` (padrão) |
| enabled | boolean | Habilitado (padrão `true`) |
| createdAt | LocalDateTime | Data de criação |

### Category

| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador único |
| nome | String | Nome da categoria |
| tipo | Enum | `RECEITA` ou `DESPESA` |
| user | User | Dono da categoria |

> Seed automático no registro: Alimentação, Transporte, Moradia, Lazer, Saúde, Educação, Salário, Outros.

### Transaction

| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador único |
| descricao | String | Descrição do lançamento |
| valor | BigDecimal | Valor (sempre positivo) |
| data | LocalDate | Data da transação |
| formaPagamento | Enum | `CARTAO_CREDITO`, `CARTAO_DEBITO`, `PIX`, `DINHEIRO`, `BOLETO`, `TRANSFERENCIA` |
| category | Category | Categoria associada (determina RECEITA/DESPESA) |
| invoice | Invoice | Fatura de origem (null se lançamento manual) |
| user | User | Dono da transação |
| createdAt | LocalDateTime | Auditoria |
| updatedAt | LocalDateTime | Auditoria |

### Invoice

| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador único |
| arquivo | String | Path relativo do PDF em `./uploads/{userId}/` |
| origem | Enum | `NUBANK`, `ITAU`, `OUTROS` |
| status | Enum | `PENDENTE_PARSE`, `PROCESSADA`, `ERRO` |
| mesReferencia | YearMonth | Mês da fatura |
| totalValor | BigDecimal | Valor total da fatura |
| transactions | List\<Transaction\> | Lançamentos gerados pelo parser |
| user | User | Dono da fatura |
| createdAt | LocalDateTime | Auditoria |

> PDFs armazenados no filesystem local; bytes não são persistidos no banco.

### Budget

| Campo | Tipo | Descrição |
|---|---|---|
| id | UUID | Identificador único |
| limite | BigDecimal | Limite planejado para o mês |
| category | Category | Categoria do orçamento |
| mes | Integer | Mês (1–12) |
| ano | Integer | Ano |
| user | User | Dono do orçamento |

> Constraint `UNIQUE(user_id, category_id, mes, ano)` no banco.

---

## Endpoints da API

### Auth

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| POST | `/auth/register` | Pública | Cadastro + seed de categorias padrão |
| POST | `/auth/login` | Pública | Login e geração de JWT (24h) |

### Transactions

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/transactions` | JWT | Lista paginada com filtros |
| POST | `/transactions` | JWT | Cria transação manual |
| PUT | `/transactions/{id}` | JWT | Atualiza transação |
| DELETE | `/transactions/{id}` | JWT | Remove transação |

Filtros disponíveis em `GET /transactions`: `?ref=YYYY-MM`, `categoryId`, `tipo`, `from`, `to`, `page`, `size`, `sort`.

### Categories

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/categories` | JWT | Lista categorias do usuário |
| POST | `/categories` | JWT | Cria categoria |
| PUT | `/categories/{id}` | JWT | Atualiza categoria |
| DELETE | `/categories/{id}` | JWT | Remove categoria |

### Invoices

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| POST | `/invoices/upload` | JWT | Upload de fatura PDF (multipart) |
| GET | `/invoices` | JWT | Lista faturas do usuário |
| GET | `/invoices/{id}` | JWT | Detalhe da fatura + transações geradas |

### Budgets

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/budgets?ref=YYYY-MM` | JWT | Lista orçamentos do mês |
| POST | `/budgets` | JWT | Cria orçamento mensal |
| PUT | `/budgets/{id}` | JWT | Atualiza orçamento |

### Reports

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/reports/summary?ref=YYYY-MM` | JWT | Resumo: receitas, despesas, orçado, saldo |
| GET | `/reports/by-category?ref=YYYY-MM` | JWT | Gastos agrupados por categoria |
| GET | `/reports/evolution?from=YYYY-MM&to=YYYY-MM` | JWT | Evolução mensal no período |
| GET | `/reports/estimate` | JWT | Projeção do mês atual (média dos últimos 3 meses) |

### Actuator

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/actuator/health` | Pública | Health check |
| GET | `/actuator/info` | Pública | Informações do build |

---

## Perfis Spring

| Perfil | Banco | ddl-auto | Flyway | Uso |
|---|---|---|---|---|
| `dev` (padrão) | PostgreSQL localhost:5432 | `create-drop` | desabilitado | Desenvolvimento local |
| `test` | H2 in-memory | `create-drop` | desabilitado | Testes automatizados |
| `prod` | PostgreSQL via env vars | `validate` | habilitado | Produção |

### Variáveis de ambiente do perfil `prod`

| Variável | Descrição |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Segredo para assinar tokens JWT |

---

## Segurança

- **Autenticação:** JWT com expiração de 24 horas via JJWT 0.12.6.
- **Filtro:** `JwtAuthFilter` intercepta todas as requisições e valida o token antes de liberar acesso ao contexto Spring Security.
- **Senhas:** hash BCrypt.
- **Autorização:** todos os endpoints (exceto `/auth/**`, `/actuator/health/**`, `/actuator/info`, `/swagger-ui/**`, `/v3/api-docs/**`) exigem token JWT válido.
- **Isolamento:** cada usuário acessa apenas seus próprios dados; verificação feita na camada de serviço usando o usuário autenticado do `SecurityContext`.

---

## Tratamento de erros

- `GlobalExceptionHandler` (`@RestControllerAdvice`) captura exceções e retorna `ProblemDetail` conforme RFC 7807.
- Erros de validação (`@Valid`) retornam HTTP 400 com detalhes dos campos inválidos.
- JSON inválido ou corpo ausente retorna HTTP 400 em `application/problem+json`.
- Métodos HTTP e `Content-Type` não suportados retornam HTTP 405/415 em `application/problem+json`.
- Falhas de autenticação retornam HTTP 401 em `application/problem+json`, inclusive quando bloqueadas pelo Spring Security antes de chegar ao controller.
- Recursos não encontrados retornam HTTP 404.
- E-mail já cadastrado retorna HTTP 409.
- Erros inesperados retornam HTTP 500.

---

## Validação

- Todos os payloads de entrada utilizam `@Valid` nos controllers.
- DTOs anotados com `@NotBlank`, `@NotNull`, `@Positive`, `@Email`, `@Size`, etc.
- Constraints de banco (UNIQUE, NOT NULL) reforçam as validações da camada de serviço.

---

## Cache

- **Caffeine Cache** aplicado nos endpoints de `/reports/*`.
- Invalidação automática ao criar, editar ou remover uma `Transaction` ou `Budget`.
- Configuração em `com.goodfunds.config`.

---

## Parser de faturas PDF

- Interface `InvoiceParser` define o contrato de parsing.
- `NubankInvoiceParser` implementa o parse de faturas Nubank com Apache PDFBox 3.0.3.
- Estratégia de seleção por `origem` da `Invoice` (padrão Strategy); arquitetura preparada para adicionar novos parsers.
- PDFs salvos em `./uploads/{userId}/` no filesystem local.

---

## Documentação de API

- Swagger UI disponível em `http://localhost:8080/swagger-ui.html` (via Springdoc OpenAPI 3 2.6.0).
- Especificação OpenAPI em `http://localhost:8080/v3/api-docs`.

---

## Como executar

**Pré-requisitos:** Java 17+, PostgreSQL rodando em `localhost:5432` com database `goodfunds`.

```bash
cd backend
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080` com perfil `dev`.

**Health check:**
```bash
curl http://localhost:8080/actuator/health
```

---

## Como testar

```bash
cd backend
./mvnw verify
```

Os testes usam o perfil `test` com H2 in-memory (não requerem PostgreSQL).

---

## Dependências principais

| Dependência | Versão | Finalidade |
|---|---|---|
| spring-boot-starter-web | 3.3.4 | API REST |
| spring-boot-starter-security | 3.3.4 | Autenticação e autorização |
| spring-boot-starter-data-jpa | 3.3.4 | Persistência |
| spring-boot-starter-validation | 3.3.4 | Bean Validation |
| spring-boot-starter-actuator | 3.3.4 | Observabilidade |
| flyway-core + flyway-database-postgresql | 3.3.4 | Migrations |
| postgresql | runtime | Driver PostgreSQL |
| h2 | runtime | Banco in-memory para testes |
| jjwt-api/impl/jackson | 0.12.6 | JWT |
| springdoc-openapi-starter-webmvc-ui | 2.6.0 | Swagger UI |
| pdfbox | 3.0.3 | Parser de faturas PDF |
| lombok | opcional | Redução de boilerplate |

---

## Decisões técnicas

| Decisão | Motivo |
|---|---|
| `open-in-view=false` | Evita lazy loading fora da transação e problemas de performance |
| `ddl-auto=create-drop` em dev/test | Simplifica desenvolvimento antes das migrations Flyway |
| `ddl-auto=validate` em prod | Flyway é a única fonte verdade do schema em produção |
| PDFs no filesystem | Evita inflar o banco com blobs binários |
| `ProblemDetail` (RFC 7807) | Formato padronizado e interoperável de erros HTTP |
| Caffeine em `/reports/*` | Relatórios são leitura intensiva; cache reduz carga no banco |
| JJWT sem refresh token | Simplificação intencional para o MVP |
