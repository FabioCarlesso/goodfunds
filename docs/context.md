# Goodfunds - Contexto Geral

## Objetivo

Goodfunds e um sistema de controle financeiro pessoal para uso proprio. O MVP deve cobrir avaliacao de faturas, planejamento financeiro, estimativas, dashboards e gestao de transacoes e categorias.

## Estrutura do repositorio

```text
goodfunds/
├── backend/                  # Backend Spring Boot (com Dockerfile multi-stage)
├── docs/                     # Contextos, planejamento e documentacao
├── frontend/                 # Frontend Vite + React + TypeScript + Tailwind (futuro)
├── docker-compose.yml        # Orquestracao local (app + postgres)
└── .env.example              # Template das variaveis de ambiente do compose
```

No estado atual, apenas o backend foi iniciado.

## Documentos principais

Documentacao tecnica (criada na issue #35):

- `docs/doc.md`: visao geral do sistema, arquitetura, modulos, fluxos e regras de negocio.
- `docs/doc-backend.md`: documentacao tecnica completa do backend.
- `docs/doc-frontend.md`: documentacao do frontend planejado.
- `docs/index.html`: documentacao visual — abrir no navegador.

Contexto e planejamento:

- `docs/context.md`: contexto geral do produto e do repositorio (este arquivo).
- `docs/context-backend.md`: contexto operacional do backend.
- `docs/context-frontend.md`: contexto planejado para o frontend.
- `docs/goodfunds-planejamento.md`: planejamento completo do MVP.

## Stack definida

- Backend: Java 17, Spring Boot 3, Maven, Spring Security, JPA/Hibernate, Flyway, Actuator.
- Banco: PostgreSQL para desenvolvimento/producao futura e H2 in-memory para testes/bootstrap.
- API docs: Swagger/OpenAPI via Springdoc.
- PDF: Apache PDFBox para parser de faturas.
- Frontend: Vite, React, TypeScript e Tailwind em sprint futura.
- Testes: JUnit 5 no backend.
- Infra: Docker Compose com servicos `app` e `postgres` para execucao local end-to-end (issue #11).

## Estado atual

- Backend criado em `backend/`.
- Maven Wrapper versionado e com checksum da distribuicao Maven.
- Smoke test do contexto Spring passando.
- Perfis `dev`, `test` e `prod` configurados em `application.yml` (issue #3).
- Perfil `dev`: PostgreSQL local (porta 5432). Perfil `test`: H2 in-memory. Perfil `prod`: variaveis de ambiente.
- Flyway habilitado em todos os perfis. Migration `V1__init.sql` cria as 5 tabelas: `users`, `categories`, `invoices`, `transactions`, `budgets` (issue #4); `V2__add_audit_columns.sql` adiciona auditoria em `budgets`/`invoices`; `V3__invoice_parse_fields_nullable.sql` permite `mes_referencia` e `total_valor` nulos ate o parse.
- Estrutura de pacotes `com.goodfunds` criada com `package-info.java` em cada pacote (issue #2).
- Entidades JPA e repositories criados para `User`, `Category`, `Invoice`, `Transaction` e `Budget`.
- Autenticacao JWT implementada: `POST /auth/register` cria usuario e 8 categorias padrao, `POST /auth/login` valida credenciais, tokens Bearer de 24h, senhas com BCrypt, rotas publicas de auth/actuator/swagger e demais rotas protegidas.
- CRUD de `Category` implementado (issue #13): `GET /categories` (lista ordenada por `nome ASC`, com filtro opcional `tipo`), `POST/PUT/DELETE /categories[/{id}]`; todas as rotas escopadas pelo usuario do JWT. `DELETE` retorna 409 `category-in-use` quando ha transacoes ou orcamentos referenciando a categoria.
- CRUD de `Transaction` implementado (issue #12): `GET /transactions` paginado com filtros (`ref`, `categoryId`, `tipo`, `from`, `to`, `page`, `size`, `sort`), `POST/PUT/DELETE /transactions[/{id}]`; todas as rotas escopadas pelo usuario do JWT e com validacao de propriedade da categoria. `size` capeado em 100, `sort` restrito a allowlist (`data`, `valor`, `descricao`, `createdAt`, `updatedAt`, `formaPagamento`), `ref` mutuamente exclusivo com `from`/`to` e `from <= to`.
- Upload de faturas implementado (issue #14): `POST /invoices/upload` autenticado recebe multipart `file` PDF e `origem` opcional (default `NUBANK`), valida content type e assinatura `%PDF`, salva em `{app.uploads.dir}/{userId}/{uuid}.pdf` (`APP_UPLOADS_DIR`, default `./uploads`) e cria `Invoice` com `status=PENDENTE_PARSE`; `mesReferencia` e `totalValor` ficam nulos ate o parser. Arquivo salvo e removido se a persistencia falhar ou a transacao fizer rollback.
- Parser de fatura Nubank implementado (issue #15) em `com.goodfunds.invoice.parser`: interface `InvoiceParser`, DTOs imutaveis `ParsedInvoice`/`ParsedInvoiceTransaction`, excecao `InvoiceParseException` e `InvoiceParserFactory` (Spring `@Component`) que seleciona a implementacao pela `OrigemFatura`. `NubankInvoiceParser` usa Apache PDFBox 3.0.3 e extrai mes de referencia, total e lancamentos. ITAU/OUTROS ainda sem parser registrado (`forOrigem` falha com `InvoiceParseException`).
- Testes de backend cobrem schema/migrations, mapeamentos JPA, geracao/validacao de JWT, fluxos HTTP de autenticacao, testes unitarios do seed de categorias padrao em `AuthService`, testes unitarios do `TransactionService`, integracao HTTP completa do CRUD de Transactions, CRUD de Categories, upload de Invoices, e parsers de fatura PDF em `NubankInvoiceParserTest`/`InvoiceParserFactoryTest`.
- Documentacao tecnica criada em `docs/` (issue #35).
- Docker Compose basico criado na raiz (issue #11) com servicos `app` (Spring Boot, build multi-stage via `backend/Dockerfile`) e `postgres` (PostgreSQL 16, volume nomeado `goodfunds-postgres-data`). Variaveis de ambiente em `.env` (template em `.env.example`).
- Frontend ainda nao foi criado.

## Proximos passos gerais

- Integrar o parser Nubank ao fluxo de upload (atualizar `Invoice` com `mesReferencia`/`totalValor`/`status=PROCESSADA` e gerar `Transaction`s), implementar listagem/detalhe de Invoices, parsers de ITAU/OUTROS, alem de Budgets e Reports, sobre a autenticacao existente.
- Criar frontend quando a sprint correspondente iniciar.
