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
| mesReferencia | YearMonth | Mês da fatura (nullable; preenchido pelo parser) |
| totalValor | BigDecimal | Valor total da fatura (nullable; preenchido pelo parser) |
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
| PATCH | `/transactions/{id}/category` | JWT | Recategoriza transação (`{ categoryId }`) |
| DELETE | `/transactions/{id}` | JWT | Remove transação |

Filtros disponíveis em `GET /transactions`: `?ref=YYYY-MM`, `categoryId`, `tipo`, `from`, `to`, `page`, `size`, `sort`.

### Categories

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/categories` | JWT | Lista categorias do usuário (ordenadas por `nome ASC`; filtro opcional `?tipo=RECEITA\|DESPESA`) |
| POST | `/categories` | JWT | Cria categoria (201 + `Location`) |
| PUT | `/categories/{id}` | JWT | Atualiza categoria (200) |
| DELETE | `/categories/{id}` | JWT | Remove categoria (204; 409 `category-in-use` se houver transações ou orçamentos associados) |

Body de `POST/PUT`: `{ "nome": "Lazer", "tipo": "DESPESA" }`. `nome` obrigatório (máx 255, é trimado); `tipo` obrigatório (`RECEITA` ou `DESPESA`). Categorias são escopadas pelo usuário do JWT — IDs inexistentes ou de outro usuário retornam 404 `category-not-found`.

### Invoices

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| POST | `/invoices/upload` | JWT | Upload de fatura PDF (multipart; 201 + `Location`) |

`POST /invoices/upload` recebe `multipart/form-data` com:

- `file` (obrigatório): arquivo PDF (`application/pdf`, com assinatura `%PDF`).
- `origem` (opcional, default `NUBANK`): valores do enum `OrigemFatura` (`NUBANK`, `ITAU`, `OUTROS`).

O arquivo é salvo em `{app.uploads.dir}/{userId}/{uuid}.pdf` e a `Invoice` é persistida com `status = PENDENTE_PARSE`. Os campos `mesReferencia` e `totalValor` ficam nulos até o parser processar a fatura. Se a persistência falhar ou a transação fizer rollback depois da gravação do PDF, o arquivo recém-salvo é removido. Limites controlados por `spring.servlet.multipart.max-file-size` (default 10MB) — excedeu retorna 413 `max-upload-size-exceeded`. Validações de arquivo retornam 400 `invalid-invoice-file`.

Listagem e detalhe de faturas (`GET /invoices` e `GET /invoices/{id}`) ainda são endpoints planejados.

### Budgets

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/budgets?ref=YYYY-MM` | JWT | Lista orçamentos do mês (ordenados por `nome` da categoria ASC) |
| POST | `/budgets` | JWT | Cria orçamento mensal (201 + `Location`) |
| PUT | `/budgets/{id}` | JWT | Atualiza orçamento (200) |

Body de `POST/PUT`: `{ "limite": 500.00, "categoryId": "<uuid>", "mes": 5, "ano": 2026 }`. `limite` obrigatório e positivo (até 2 casas decimais); `categoryId` obrigatório; `mes` obrigatório (1–12); `ano` obrigatório (2000–2100). O parâmetro `ref` em `GET /budgets` é obrigatório (`yyyy-MM`); se ausente retorna 400 `validation-error` (`errors.ref`); se mal formatado (ex: `2026-13`) também retorna 400 `validation-error`. Orçamentos são escopados pelo usuário do JWT — `id` ou `categoryId` inexistentes ou de outro usuário retornam 404 (`budget-not-found` / `category-not-found`). A combinação `(usuário, categoria, mês, ano)` é única: tentar criar ou mover um orçamento para um período já ocupado retorna 409 `budget-already-exists`.

### Reports

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/reports/summary?ref=YYYY-MM` | JWT | Resumo: receitas, despesas, orçado, saldo |
| GET | `/reports/by-category?ref=YYYY-MM` | JWT | Gastos agrupados por categoria |
| GET | `/reports/evolution?from=YYYY-MM&to=YYYY-MM` | JWT | Evolução mensal no período |
| GET | `/reports/estimate` | JWT | Projeção do mês atual (média dos últimos 3 meses) |

`GET /reports/estimate` (issue #20) já está implementado; os demais relatórios continuam planejados. A engine `EstimateService` calcula, por categoria do usuário do JWT, a **média dos últimos 3 meses fechados** (soma dos lançamentos no período `[mêsAtual-3 .. mêsAtual-1]` dividida por 3 — meses sem lançamento contam como zero) e a **projeção do mês corrente**, que extrapola o realizado parcial para o mês inteiro pela fórmula `realizado * (diasNoMes / diasDecorridos)`. A resposta traz a data de referência (`ref`, `yyyy-MM`), `diasNoMes`, `diasDecorridos`, o bloco `consolidado` (`media`, `realizado`, `projecao` somados sobre todas as categorias) e a lista `categorias` (mesmos campos por categoria, ordenada por nome; apenas categorias com lançamento no período fechado ou no mês corrente aparecem). A data corrente vem de um bean `Clock` (`ClockConfig`), permitindo relógio fixo nos testes. Cobertura: `EstimateServiceTest` (unitário: sem histórico, histórico parcial, histórico completo e consolidação multi-categoria) e `ReportEstimateControllerIntegrationTest` (HTTP, com relógio fixo, isolamento por usuário e 401 sem token).

### Actuator

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/actuator/health` | Pública | Health check |
| GET | `/actuator/info` | Pública | Informações do build |

---

## Perfis Spring

| Perfil | Banco | ddl-auto | Flyway | Uso |
|---|---|---|---|---|
| `dev` (padrão) | PostgreSQL localhost:5432 | `none` | habilitado | Desenvolvimento local |
| `test` | H2 in-memory (MODE=PostgreSQL) | `none` | habilitado | Testes automatizados |
| `prod` | PostgreSQL via env vars | `validate` | habilitado | Produção |

### Variáveis de ambiente do perfil `prod`

| Variável | Descrição |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Segredo para assinar tokens JWT |
| `APP_UPLOADS_DIR` | Diretório onde os PDFs de fatura são salvos (default `./uploads`) |

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
- Orçamento duplicado para a mesma `(categoria, mês, ano)` retorna HTTP 409 `budget-already-exists` (verificação na camada de serviço; a constraint UNIQUE no banco atua como backstop e violações por concorrência também são mapeadas para 409 via handler de `DataIntegrityViolationException`).
- Erros de regra de negócio específicos usam o status HTTP semântico correspondente (ex: 409 para e-mail duplicado); HTTP 422 não é utilizado.
- Erros inesperados retornam HTTP 500.

---

## Validação

- Todos os payloads de entrada utilizam `@Valid` nos controllers.
- DTOs anotados com `@NotBlank`, `@NotNull`, `@Positive`, `@Email`, `@Size`, etc.
- Constraints de banco (UNIQUE, NOT NULL) reforçam as validações da camada de serviço.

---

## Cache

Cache **Caffeine** aplicado aos relatórios (`/reports/*`), implementado na issue #23. Relatórios são leitura intensiva e recalculam agregações sobre transações e orçamentos a cada chamada; o cache evita reexecutar essas consultas enquanto os dados do usuário não mudam.

- **Configuração:** `com.goodfunds.config.CacheConfig` (`@EnableCaching` + `CaffeineCacheManager`). Quatro caches, um por endpoint: `reportsSummary`, `reportsByCategory`, `reportsEvolution`, `reportsEstimate`. Política: `expireAfterWrite = 10min` e `maximumSize = 1000` por cache.
- **Ordem dos interceptors:** `@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)` coloca o interceptor de cache à frente do de transação, para que um *cache hit* retorne sem abrir a transação read-only nem tomar uma conexão do pool.
- **Onde:** `@Cacheable` nos métodos de serviço `ReportService.summary/byCategory/evolution` e `EstimateService.estimate`.
- **Chave por usuário:** todas as chaves começam com `"<userId>::..."` (ex.: `summary` → `"<userId>::<ref>"`, `estimate` → `"<userId>::estimate"`), garantindo isolamento entre usuários no cache.
- **Invalidação por usuário:** `com.goodfunds.service.ReportCacheService.evictUser(userId)` remove apenas as entradas do usuário (varre as chaves do cache nativo Caffeine pelo prefixo `"<userId>::"`), sem descartar o cache dos demais. É acionada ao criar, editar ou remover uma `Transaction` (`TransactionService`), criar ou editar um `Budget` (`BudgetService`), editar ou remover uma `Category` (`CategoryService` — renomear ou trocar o tipo `DESPESA`↔`RECEITA` altera `summary`/`by-category`/`estimate`; a criação não invalida porque uma categoria nova ainda não tem lançamentos), e ao processar uma fatura que gera/remove transações (`InvoiceProcessingService`).
- **Invalidação após o commit:** havendo transação ativa, a remoção é registrada em `afterCommit` (`TransactionSynchronization`); isso fecha a janela em que uma leitura concorrente do mesmo usuário leria as linhas antigas (ainda commitadas) e repovoaria o cache com dados obsoletos. Sem transação ativa (ex.: chamada direta), a remoção é imediata.
- **`expireAfterWrite` como rede de segurança:** garante o recálculo de entradas dependentes do "mês corrente" (ex.: `summary` sem `ref` e `estimate`) após a virada do mês, mesmo sem escrita.

---

## Parser de faturas PDF

- O upload de PDFs cria `Invoice` com `status = PENDENTE_PARSE` (campos `mesReferencia` e `totalValor` nulos até o parser rodar).
- Parsers vivem em `com.goodfunds.invoice.parser`:
  - Interface `InvoiceParser` com `OrigemFatura origem()` e `ParsedInvoice parse(File pdf)`.
  - DTOs imutáveis `ParsedInvoice` (`mesReferencia`, `total`, `transacoes`) e `ParsedInvoiceTransaction` (`data`, `descricao`, `valor`).
  - `InvoiceParseException` para erros de leitura/extração.
  - `InvoiceParserFactory` (Spring `@Component`) recebe todos os `InvoiceParser` via injeção e expõe `forInvoice(Invoice)` / `forOrigem(OrigemFatura)`. Garante que cada origem tenha no máximo uma implementação registrada.
- `NubankInvoiceParser` (MVP): usa Apache PDFBox 3.0.3 (`Loader.loadPDF` + `PDFTextStripper`) e extrai do texto:
  - **Mês de referência:** linha `Mês de referência: <MES> de <ANO>` (mês em PT-BR, três letras: `JAN`...`DEZ`).
  - **Total:** linha `Valor total: R$ <valor>` ou `Total da fatura: R$ <valor>`.
  - **Lançamentos:** linhas `DD MMM Descrição R$ <valor>`. A data é montada com o ano da fatura; meses posteriores ao mês de referência são interpretados como ano anterior (transações anteriores à data de fechamento).
- A seleção por `origem` (`NUBANK`, `ITAU`, `OUTROS`) deixa o modelo pronto para novos parsers — basta criar mais uma implementação `InvoiceParser` anotada com `@Component`.
- Fixture de teste: `src/test/resources/invoices/nubank/sample-fatura.pdf`, gerada deterministicamente por `NubankInvoiceFixtures` (regenerável via `java ... com.goodfunds.invoice.parser.NubankInvoiceFixtures`).
- PDFs salvos em `{app.uploads.dir}/{userId}/` no filesystem local.

### Geração de transações a partir da fatura (issue #16)

- `InvoiceProcessingService.process(userId, invoiceId)` orquestra o pós-upload, **escopado pelo usuário autenticado**: valida que a fatura pertence ao `userId` (caso contrário `InvoiceNotFoundException`, sem vazar a existência de faturas de outros usuários), localiza o PDF em `{app.uploads.dir}/{arquivo}` (validando que o caminho resolvido permanece dentro do diretório base), escolhe o parser via `InvoiceParserFactory.forInvoice(...)` e converte cada `ParsedInvoiceTransaction` em uma `Transaction`.
- Cada `Transaction` gerada recebe `invoice = <id da fatura>`, `formaPagamento = CARTAO_CREDITO`, `user` do dono da fatura e categoria padrão `Outros` (categoria semeada por usuário no registro; resolvida de forma determinística por `CategoryRepository.findFirstByUserIdAndNomeIgnoreCaseOrderByIdAsc`). A categoria padrão será substituída por sugestão automática numa issue futura.
- Antes de persistir, os lançamentos extraídos são validados: `valor > 0` e `descricao` não vazia com no máximo 500 caracteres (alinhado com `@Positive`/`CHECK (valor > 0)` e `VARCHAR(500)`). Lançamentos inválidos — por exemplo linhas de crédito/estorno com valor negativo — fazem o processamento falhar de forma controlada (`status = ERRO`) em vez de estourar uma constraint no commit.
- Em caso de sucesso, a fatura recebe `mesReferencia`/`totalValor` extraídos e `status = PROCESSADA`. Falhas de parse, de validação dos lançamentos ou de resolução da categoria padrão são logadas e marcam `status = ERRO` (sem propagar rollback), permitindo nova tentativa.
- **Idempotência:** faturas já `PROCESSADA` retornam sem reprocessar; um reprocessamento explícito remove os lançamentos anteriores da fatura (bulk delete `@Modifying`) antes de recriar, garantindo que `process` não duplique transações.
- Fatura inexistente (ou de outro usuário) resulta em `InvoiceNotFoundException` (404 `invoice-not-found`).

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
| spring-boot-starter-cache | 3.3.4 | Abstração de cache (`@Cacheable`/`@EnableCaching`) |
| caffeine | gerenciado pelo BOM | Cache em memória dos relatórios `/reports/*` |
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
| `ddl-auto=none` em dev/test | Flyway gerencia o schema também em desenvolvimento e testes |
| `ddl-auto=validate` em prod | Flyway é a única fonte verdade do schema em produção |
| PDFs no filesystem | Evita inflar o banco com blobs binários |
| `ProblemDetail` (RFC 7807) | Formato padronizado e interoperável de erros HTTP |
| Caffeine em `/reports/*` | Relatórios são leitura intensiva; cache reduz carga no banco |
| JJWT sem refresh token | Simplificação intencional para o MVP |
