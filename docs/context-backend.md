# Goodfunds - Contexto do Backend

## Estado atual

O repositorio contem o bootstrap inicial do backend em `backend/`, criado com Spring Boot 3.3.4, Java 17 e Maven Wrapper. A aplicacao principal esta em `com.goodfunds.GoodfundsApplication`.

- Build: Maven, via `backend/mvnw`.
- Java: 17 configurado no `pom.xml`.
- Spring Boot: 3.3.4.
- Perfis configurados: `dev` (PostgreSQL local), `test` (H2 in-memory) e `prod` (variaveis de ambiente).
- Perfil padrao: `dev`.
- Flyway: dependencia incluida, habilitado apenas em `prod`. Sera habilitado nos demais perfis na issue #4 (primeira migration).
- PostgreSQL: perfis `dev` e `prod` usam PostgreSQL. Credenciais dev tem defaults locais; prod le de variaveis de ambiente.
- Actuator: expostos apenas `health` e `info`.
- JPA: `open-in-view=false` na base. `ddl-auto=create-drop` em dev e test; `validate` em prod.
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
| `dev` (padrao) | PostgreSQL local (porta 5432) | `create-drop` | desabilitado | Desenvolvimento local |
| `test` | H2 in-memory | `create-drop` | desabilitado | Testes automatizados (`./mvnw verify`) |
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

O teste atual e um smoke test de contexto Spring com profile `test`.

## Decisoes temporarias

- Flyway permanece desabilitado em `dev` e `test` ate a criacao do `V1__init.sql` (issue #4).
- `ddl-auto=create-drop` em `dev` e `test` enquanto nao ha migrations. Sera substituido por `none` apos Flyway ativado.
- Spring Security esta no classpath para a issue de JWT, mas ainda nao ha configuracao de seguranca da aplicacao.
- PDFBox, Springdoc e JJWT ja estao no classpath porque fazem parte das dependencias base do MVP e serao usados em issues futuras.

## Proximos passos

- Habilitar Flyway e criar `V1__init.sql` (issue #4).
- Adicionar entidades JPA, repositories, services, controllers e configuracao de seguranca JWT.
- Expandir testes alem do smoke test conforme funcionalidades forem implementadas.
