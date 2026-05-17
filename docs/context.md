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
- Flyway habilitado em todos os perfis. Migration `V1__init.sql` cria as 5 tabelas: `users`, `categories`, `invoices`, `transactions`, `budgets` (issue #4).
- Estrutura de pacotes `com.goodfunds` criada com `package-info.java` em cada pacote (issue #2).
- Entidades JPA e repositories criados para `User`, `Category`, `Invoice`, `Transaction` e `Budget`.
- Autenticacao JWT implementada: `POST /auth/register` cria usuario e 8 categorias padrao, `POST /auth/login` valida credenciais, tokens Bearer de 24h, senhas com BCrypt, rotas publicas de auth/actuator/swagger e demais rotas protegidas.
- CRUD de `Category` implementado (issue #13): `GET /categories` (lista ordenada por `nome ASC`, com filtro opcional `tipo`), `POST/PUT/DELETE /categories[/{id}]`; todas as rotas escopadas pelo usuario do JWT. `DELETE` retorna 409 `category-in-use` quando ha transacoes ou orcamentos referenciando a categoria.
- CRUD de `Transaction` implementado (issue #12): `GET /transactions` paginado com filtros (`ref`, `categoryId`, `tipo`, `from`, `to`, `page`, `size`, `sort`), `POST/PUT/DELETE /transactions[/{id}]`; todas as rotas escopadas pelo usuario do JWT e com validacao de propriedade da categoria. `size` capeado em 100, `sort` restrito a allowlist (`data`, `valor`, `descricao`, `createdAt`, `updatedAt`, `formaPagamento`), `ref` mutuamente exclusivo com `from`/`to` e `from <= to`.
- Testes de backend cobrem schema/migrations, mapeamentos JPA, geracao/validacao de JWT, fluxos HTTP de autenticacao, testes unitarios do seed de categorias padrao em `AuthService`, testes unitarios do `TransactionService` e integracao HTTP completa do CRUD de Transactions.
- Documentacao tecnica criada em `docs/` (issue #35).
- Docker Compose basico criado na raiz (issue #11) com servicos `app` (Spring Boot, build multi-stage via `backend/Dockerfile`) e `postgres` (PostgreSQL 16, volume nomeado `goodfunds-postgres-data`). Variaveis de ambiente em `.env` (template em `.env.example`).
- Frontend ainda nao foi criado.

## Proximos passos gerais

- Implementar services e controllers de Invoices, Budgets e Reports sobre a autenticacao existente (Transactions implementado na issue #12, Categories na issue #13).
- Criar frontend quando a sprint correspondente iniciar.
