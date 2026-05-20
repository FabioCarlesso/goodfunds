# Goodfunds - Contexto do Backend

## Estado atual

O repositorio contem o bootstrap inicial do backend em `backend/`, criado com Spring Boot 3.3.4, Java 17 e Maven Wrapper. A aplicacao principal esta em `com.goodfunds.GoodfundsApplication`.

- Build: Maven, via `backend/mvnw`.
- Java: 17 configurado no `pom.xml`.
- Spring Boot: 3.3.4.
- Perfis configurados: `dev` (PostgreSQL local), `test` (H2 in-memory) e `prod` (variaveis de ambiente).
- Perfil padrao: `dev`.
- Flyway: habilitado em todos os perfis. Migration `V1__init.sql` cria as 5 tabelas: `users`, `categories`, `invoices`, `transactions`, `budgets`; `V2__add_audit_columns.sql` adiciona colunas de auditoria em `budgets` e `invoices`; `V3__invoice_parse_fields_nullable.sql` torna `invoices.mes_referencia` e `invoices.total_valor` nullable para o fluxo de parse posterior.
- Entidades JPA: `User`, `Category`, `Invoice`, `Transaction` e `Budget` mapeadas em `com.goodfunds.domain`, com enums (`Role`, `TipoCategoria`, `FormaPagamento`, `OrigemFatura`, `StatusFatura`) e repositorios Spring Data JPA em `com.goodfunds.repository`. UUIDs gerados via `GenerationType.UUID`; timestamps com `@CreationTimestamp`/`@UpdateTimestamp`; `mesReferencia` mapeado para `YearMonth` via `AttributeConverter`.
- PostgreSQL: perfis `dev` e `prod` usam PostgreSQL. Credenciais dev tem defaults locais; prod le de variaveis de ambiente.
- Actuator: expostos apenas `health` e `info`; `/actuator/health` e subpaths de health sao publicos, e `show-details: never` garante que detalhes internos de saude nao sao exibidos publicamente (issue #8).
- JPA: `open-in-view=false` na base. `ddl-auto=none` em dev e test (Flyway gerencia o schema); `validate` em prod.
- JWT: secret configurado via `jwt.secret` e expiracao via `jwt.expiration` (default 24h). Em dev usa valor padrao; em prod obrigatorio via `JWT_SECRET`.
- Autenticacao: `JwtService` (JJWT 0.12.6) emite tokens com expiracao de 24h; `JwtAuthenticationFilter` valida o header `Authorization: Bearer ...` e popula o `SecurityContext`. `SecurityConfig` deixa publicas as rotas `/auth/**`, `/actuator/health/**`, `/actuator/info` e Swagger; demais exigem token valido (sessao stateless). `CustomUserDetailsService` carrega usuario por email. Senhas com BCrypt.
- Endpoints publicos `POST /auth/register` (201, cria usuario + 8 categorias padrao) e `POST /auth/login` (200, valida credenciais), ambos retornando `{ token, tokenType: "Bearer", expiresInMillis }`. Email e normalizado para lowercase.
- Endpoints autenticados de `Category` (issue #13): `GET /categories` (lista ordenada por `nome ASC`, com filtro opcional `tipo=RECEITA|DESPESA`), `POST /categories` (201 com header `Location`), `PUT /categories/{id}` (200), `DELETE /categories/{id}` (204). Todos escopados pelo usuario do JWT — categoria inexistente ou de outro usuario retorna 404 `category-not-found`. Validacoes: `nome` obrigatorio (max 255, trimado antes de persistir), `tipo` obrigatorio. `DELETE` retorna 409 `category-in-use` se houver transacoes ou orcamentos referenciando a categoria (pre-check antes do `delete`, alinhado com FK `ON DELETE RESTRICT`).
- Endpoints autenticados de `Transaction` (issue #12): `GET /transactions` (Page com filtros opcionais `ref=YYYY-MM`, `categoryId`, `tipo`, `from`, `to`, `page`, `size`, `sort`; default `sort=data,desc`, `size=20`), `POST /transactions` (201 com header `Location`), `PUT /transactions/{id}` (200), `DELETE /transactions/{id}` (204). Todos escopados pelo usuario do JWT; categoria precisa pertencer ao usuario (caso contrario 404 `category-not-found`); transacao inexistente ou de outro usuario retorna 404 `transaction-not-found`. Validacoes: `descricao` obrigatoria (max 500), `valor > 0` com no maximo 2 casas decimais (`@Digits(integer=17, fraction=2)` para casar com `NUMERIC(19,2)`), `data`, `formaPagamento` e `categoryId` obrigatorios. Campo `invoiceId` so e populado quando a transacao foi gerada pelo parser de fatura — endpoints manuais nao o alteram (regressao coberta em `TransactionControllerIntegrationTest#update_preservesInvoiceLink`). `PUT` faz `saveAndFlush` para garantir que `@UpdateTimestamp` seja aplicado antes de mapear a resposta.
- Endpoint autenticado de upload de `Invoice` (issue #14): `POST /invoices/upload` recebe `multipart/form-data` com `file` obrigatorio (`application/pdf` e assinatura `%PDF`) e `origem` opcional (`NUBANK` default; tambem aceita `ITAU` e `OUTROS`). Salva o PDF em `{app.uploads.dir}/{userId}/{uuid}.pdf`, configurado por `APP_UPLOADS_DIR` com default `./uploads`, cria `Invoice` com `status=PENDENTE_PARSE` e `mesReferencia`/`totalValor` nulos ate o parser. O arquivo e removido se a persistencia falhar ou a transacao fizer rollback.
- Processamento de fatura (issue #16): `InvoiceProcessingService.process(userId, invoiceId)` em `com.goodfunds.service` orquestra parse -> geracao de `Transaction`s, **escopado pelo usuario** (fatura de outro usuario -> `InvoiceNotFoundException`, sem vazar a existencia). Carrega o PDF de `{app.uploads.dir}/{arquivo}` (com verificacao de contencao do path resolvido dentro do diretorio base), usa o `InvoiceParserFactory` e cria uma `Transaction` por lancamento com `invoice=<id>`, `formaPagamento=CARTAO_CREDITO`, `user` da fatura e categoria default `Outros` (resolvida de forma deterministica via `CategoryRepository.findFirstByUserIdAndNomeIgnoreCaseOrderByIdAsc`). Antes de persistir, os lancamentos extraidos sao validados (`valor > 0` e `descricao` nao vazia com ate 500 chars, alinhado com `@Positive`/`CHECK (valor > 0)` e `VARCHAR(500)`); lancamentos invalidos (ex.: credito/estorno com valor negativo) marcam `status=ERRO` de forma controlada, sem estourar constraint no commit. Sucesso preenche `mesReferencia`/`totalValor` e marca `status=PROCESSADA`; falhas de parse, validacao dos lancamentos ou resolucao de categoria sao logadas e marcam `status=ERRO` sem propagar rollback. Idempotente: fatura ja `PROCESSADA` retorna sem reprocessar e um reprocessamento explicito remove lancamentos anteriores (`TransactionRepository.deleteByInvoiceId`, bulk `@Modifying`) antes de recriar. Fatura inexistente -> `InvoiceNotFoundException` (404 `invoice-not-found`). Cobertura: `InvoiceProcessingServiceTest` (mocks, inclui escopo por usuario e valor nao positivo) e `InvoiceProcessingServiceIntegrationTest` (fixture real, idempotencia, escopo por usuario e caminho de erro).
- Parsers de fatura PDF (issue #15) em `com.goodfunds.invoice.parser`: interface `InvoiceParser` (`OrigemFatura origem()`, `ParsedInvoice parse(File pdf)`), DTOs imutaveis `ParsedInvoice`/`ParsedInvoiceTransaction`, excecao `InvoiceParseException` e `InvoiceParserFactory` (Spring `@Component`) que recebe todas as implementacoes por DI e expoe `forInvoice(Invoice)`/`forOrigem(OrigemFatura)`. Implementacao MVP: `NubankInvoiceParser` usa Apache PDFBox 3.0.3 (`Loader.loadPDF` + `PDFTextStripper`) para extrair mes de referencia (`Mês de referência: <MES> de <ANO>`), total (`Valor total: R$ ...`) e lancamentos (`DD MMM Descricao R$ valor`); meses posteriores ao mes de referencia sao recuados para o ano anterior. Fixture commitada em `backend/src/test/resources/invoices/nubank/sample-fatura.pdf`, regeravel via `NubankInvoiceFixtures` (test sources). Cobertura: `NubankInvoiceParserTest`, `InvoiceParserFactoryTest`.
- Regras de paginacao e filtros do `GET /transactions`:
  - `size` limitado a `spring.data.web.pageable.max-page-size=100` (default `default-page-size=20`). Requisicoes com `size>100` sao automaticamente truncadas.
  - Campos permitidos em `sort`: `data`, `valor`, `descricao`, `createdAt`, `updatedAt`, `formaPagamento`. Qualquer outro campo e silenciosamente descartado, evitando property-name injection via JPA Specifications; quando nenhum campo valido sobra, o servico aplica o default `data,desc`.
  - `ref` e mutuamente exclusivo com `from`/`to`. `from` deve ser `<= to`. Violacoes retornam 400 `urn:goodfunds:problem:invalid-filter`.
  - A query usa `@EntityGraph(attributePaths = {"category","invoice"})` no `findAll(Specification, Pageable)`, eliminando o N+1 ao mapear `TransactionResponse`.
- O `AuthenticatedUser` (custom `UserDetails`) carrega `userId` e `displayName`, removendo a segunda consulta a `users` por request autenticada — controllers usam `@AuthenticationPrincipal AuthenticatedUser` direto.
- Erros HTTP da API usam `ProblemDetail` (`application/problem+json`) via `GlobalExceptionHandler`, incluindo validacao de body (`validation-error` com mapa `errors`), validacao de query params (`MethodArgumentTypeMismatchException` e `MissingServletRequestParameterException` tambem produzem `validation-error`), filtros de transacao invalidos (`invalid-filter`), JSON invalido, metodo/media type nao suportados, recursos inexistentes (`resource-not-found` para rotas, `transaction-not-found`/`category-not-found` para entidades), falha de autenticacao e e-mail duplicado. Requisicoes bloqueadas pelo Spring Security tambem retornam `ProblemDetail`.

## Estrutura de pacotes

Pacotes criados sob `com.goodfunds` (cada um com `package-info.java` documentando seu papel):

- `controller`: REST controllers da API.
- `service`: regras de negocio.
- `repository`: interfaces Spring Data JPA.
- `domain`: entidades JPA e enums de dominio.
- `dto`: contratos de entrada/saida e validacoes Bean Validation.
- `config`: configuracoes transversais (cache, OpenAPI, etc.).
- `security`: Spring Security e autenticacao JWT.
- `exception`: `GlobalExceptionHandler` (`ProblemDetail` / RFC 7807) e excecoes de dominio.

## Perfis Spring

| Perfil | Banco | ddl-auto | Flyway | Uso |
|--------|-------|----------|--------|-----|
| `dev` (padrao) | PostgreSQL local (porta 5432) | `none` | habilitado | Desenvolvimento local |
| `test` | H2 in-memory (MODE=PostgreSQL) | `none` | habilitado | Testes automatizados (`./mvnw verify`) |
| `prod` | PostgreSQL via env vars | `validate` | habilitado | Producao |

Variaveis de ambiente do perfil `prod`:

| Variavel | Descricao |
|----------|-----------|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuario do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Segredo para assinar tokens JWT |

## Como executar

```bash
cd backend
./mvnw spring-boot:run
```

A aplicacao sobe em `http://localhost:8080` usando o perfil `dev` (requer PostgreSQL local na porta 5432 com database `goodfunds`).

Health check:

```bash
curl http://localhost:8080/actuator/health
```

## Como testar

```bash
cd backend
./mvnw verify
```

A suite atual roda com profile `test` e cobre smoke test de contexto Spring, validacoes de schema/constraints via `MigrationSchemaTest`, mapeamentos JPA/repositories via `JpaMappingTest`, geracao/validacao de tokens em `JwtServiceTest`, fluxo HTTP de autenticacao (register, login, erros e protecao de rotas) em `AuthControllerIntegrationTest`, testes unitarios do CRUD de transacoes em `TransactionServiceTest` (mockando os repositorios), integracao HTTP completa (paginacao, filtros, validacoes e isolamento por usuario) em `TransactionControllerIntegrationTest`, testes unitarios do CRUD de categorias em `CategoryServiceTest`, integracao HTTP completa do CRUD de categorias em `CategoryControllerIntegrationTest`, upload de faturas em `InvoiceControllerIntegrationTest`/`InvoiceServiceTest`, parsers de fatura PDF em `NubankInvoiceParserTest` (extracao de mes/total/lancamentos contra fixture binaria) e `InvoiceParserFactoryTest`, e geracao de transacoes a partir da fatura em `InvoiceProcessingServiceTest` (mocks) e `InvoiceProcessingServiceIntegrationTest` (fixture real, idempotencia e caminho de erro).

## Convencoes de schema

- `updated_at` em `transactions`, `invoices` e `budgets` usa `DEFAULT NOW()` na criacao; atualizacoes automaticas dependem de `@UpdateTimestamp` nas entidades JPA.
- Timestamps armazenados como `TIMESTAMP WITH TIME ZONE` (UTC). Hibernate configurado com `hibernate.jdbc.time_zone=UTC`.
- FKs de `user_id`: `ON DELETE CASCADE` (dado do usuario e removido junto). FKs de `category_id`: `ON DELETE RESTRICT`. FK de `invoice_id` em transactions: `ON DELETE SET NULL`.

## Decisoes temporarias

- PDFBox e Springdoc ja estao no classpath porque fazem parte das dependencias base do MVP. Upload de PDF e parser Nubank ja implementados; demais origens (`ITAU`, `OUTROS`) ainda nao tem parser registrado e farao `forOrigem` falhar com `InvoiceParseException` ate uma issue futura adicionar a implementacao.
- JWT sem refresh token no MVP (decisao explicita em `goodfunds-planejamento.md`).

## Proximos passos

- A geracao de `Transaction`s a partir da fatura ja existe (`InvoiceProcessingService`, issue #16). Falta expor o gatilho via endpoint REST e adicionar listagem/detalhe de Invoices, alem de Budgets e Reports, sobre a infraestrutura de seguranca ja existente.
- Expandir testes conforme funcionalidades forem implementadas.
