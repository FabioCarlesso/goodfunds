# Goodfunds - Contexto do Projeto

## Estado atual

O repositorio contem o bootstrap inicial do backend em `backend/`, criado com Spring Boot 3.3.4, Java 17 e Maven Wrapper. A aplicacao principal esta em `com.goodfunds.GoodfundsApplication`.

## Backend

- Build: Maven, via `backend/mvnw`.
- Java: 17 configurado no `pom.xml`.
- Spring Boot: 3.3.4.
- Banco default temporario: H2 in-memory em modo PostgreSQL.
- Flyway: dependencia incluida, mas `spring.flyway.enabled=false` enquanto as migrations iniciais nao existem.
- PostgreSQL: driver runtime e modulo `flyway-database-postgresql` ja incluidos para os proximos perfis `dev`/`prod`.
- Actuator: expostos apenas `health` e `info`.
- JPA: `open-in-view=false` e `ddl-auto=create-drop` apenas na configuracao base temporaria com H2.

## Como executar

```bash
cd backend
./mvnw spring-boot:run
```

A aplicacao sobe em `http://localhost:8080`.

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

- Nao ha profile `dev` dedicado ainda. A configuracao base usa H2 para permitir execucao local sem PostgreSQL ate a issue de profiles e Docker Compose.
- Flyway permanece desabilitado ate a criacao do `V1__init.sql`.
- Spring Security esta no classpath para a issue de JWT, mas ainda nao ha configuracao de seguranca da aplicacao.
- PDFBox, Springdoc e JJWT ja estao no classpath porque fazem parte das dependencias base do MVP e serao usados em issues futuras.

## Proximos passos

- Criar `application-dev.yml`, `application-test.yml` e `application-prod.yml` com responsabilidades claras.
- Habilitar Flyway quando a primeira migration for adicionada.
- Adicionar entidades JPA, repositories, services, controllers e configuracao de seguranca JWT.
- Expandir testes alem do smoke test conforme funcionalidades forem implementadas.
