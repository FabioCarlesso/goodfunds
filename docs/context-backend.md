# Goodfunds - Contexto do Backend

## Estado atual

O repositorio contem o bootstrap inicial do backend em `backend/`, criado com Spring Boot 3.3.4, Java 17 e Maven Wrapper. A aplicacao principal esta em `com.goodfunds.GoodfundsApplication`.

- Build: Maven, via `backend/mvnw`.
- Java: 17 configurado no `pom.xml`.
- Spring Boot: 3.3.4.
- Perfis configurados: `dev` (PostgreSQL local), `test` (H2 in-memory) e `prod` (variaveis de ambiente).
- Perfil padrao: `dev`.
- Flyway: habilitado em todos os perfis. Migration `V1__init.sql` cria as 5 tabelas: `users`, `categories`, `invoices`, `transactions`, `budgets`; `V2__add_audit_columns.sql` adiciona colunas de auditoria em `budgets` e `invoices`.
- Entidades JPA: `User`, `Category`, `Invoice`, `Transaction` e `Budget` mapeadas em `com.goodfunds.domain`, com enums (`Role`, `TipoCategoria`, `FormaPagamento`, `OrigemFatura`, `StatusFatura`) e repositorios Spring Data JPA em `com.goodfunds.repository`. UUIDs gerados via `GenerationType.UUID`; timestamps com `@CreationTimestamp`/`@UpdateTimestamp`; `mesReferencia` mapeado para `YearMonth` via `AttributeConverter`.
- PostgreSQL: perfis `dev` e `prod` usam PostgreSQL. Credenciais dev tem defaults locais; prod le de variaveis de ambiente.
- Actuator: expostos apenas `health` e `info`.
- JPA: `open-in-view=false` na base. `ddl-auto=none` em dev e test (Flyway gerencia o schema); `validate` em prod.
- JWT: secret configurado via `jwt.secret`. Em dev usa valor padrao; em prod obrigatorio via `JWT_SECRET`.

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

A suite atual roda com profile `test` e cobre smoke test de contexto Spring, validacoes de schema/constraints via `MigrationSchemaTest` e mapeamentos JPA/repositories via `JpaMappingTest`.

## Convencoes de schema

- `updated_at` em `transactions`, `invoices` e `budgets` usa `DEFAULT NOW()` na criacao; atualizacoes automaticas dependem de `@UpdateTimestamp` nas entidades JPA.
- Timestamps armazenados como `TIMESTAMP WITH TIME ZONE` (UTC). Hibernate configurado com `hibernate.jdbc.time_zone=UTC`.
- FKs de `user_id`: `ON DELETE CASCADE` (dado do usuario e removido junto). FKs de `category_id`: `ON DELETE RESTRICT`. FK de `invoice_id` em transactions: `ON DELETE SET NULL`.

## Decisoes temporarias

- Spring Security esta no classpath para a issue de JWT, mas ainda nao ha configuracao de seguranca da aplicacao.
- PDFBox, Springdoc e JJWT ja estao no classpath porque fazem parte das dependencias base do MVP e serao usados em issues futuras.

## Proximos passos

- Adicionar services, controllers e configuracao de seguranca JWT sobre as entidades e repositorios existentes.
- Expandir testes alem do smoke test conforme funcionalidades forem implementadas.
