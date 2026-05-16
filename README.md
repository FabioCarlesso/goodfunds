# Goodfunds

Goodfunds e um sistema de controle financeiro pessoal para uso proprio. O MVP cobre faturas, transacoes, categorias, planejamento financeiro, relatorios e estimativas.

## Estado atual

O repositorio esta na Sprint 1. O backend Spring Boot ja foi criado em `backend/`, com profiles `dev`, `test` e `prod`, migrations Flyway e autenticacao JWT. O Docker Compose basico (app + postgres) ja esta disponivel (issue #11). O frontend ainda sera implementado.

## Estrutura

```text
goodfunds/
├── backend/                  # Backend Spring Boot 3 + Maven (com Dockerfile)
├── docs/                     # Contextos e planejamento
├── docker-compose.yml        # Stack local: app + postgres
├── .env.example              # Template das variaveis de ambiente do compose
└── README.md
```

## Backend

Requisitos:

- Java 17+
- Bash ou terminal compativel com Maven Wrapper

Executar:

```bash
cd backend
./mvnw spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Testar:

```bash
cd backend
./mvnw verify
```

## Docker Compose

Stack local com backend Spring Boot + PostgreSQL.

Requisitos:

- Docker Engine 24+ e Docker Compose v2.

Passos:

```bash
cp .env.example .env
# edite .env e defina JWT_SECRET com um valor longo e aleatorio
docker compose up --build
```

A API sobe em `http://localhost:8080` (porta configuravel via `APP_PORT`). O PostgreSQL fica em `localhost:5432` (`POSTGRES_PORT`) e os dados ficam no volume nomeado `goodfunds-postgres-data`.

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Derrubar:

```bash
docker compose down            # para os containers, mantem o volume
docker compose down -v         # tambem apaga os dados do Postgres
```

## Documentacao

Documentacao tecnica principal (criada na issue #35):

- `docs/doc.md`: visao geral do sistema, arquitetura, modulos e fluxos principais.
- `docs/doc-backend.md`: documentacao tecnica do backend (entidades, endpoints, perfis, seguranca).
- `docs/doc-frontend.md`: documentacao do frontend planejado (telas, navegacao, integracao com a API).
- `docs/index.html`: documentacao visual do sistema — abrir direto no navegador.

Documentos de contexto e planejamento:

- `docs/context.md`: contexto geral do produto e do repositorio.
- `docs/context-backend.md`: contexto operacional do backend.
- `docs/context-frontend.md`: contexto planejado do frontend.
- `docs/goodfunds-planejamento.md`: planejamento completo do MVP.

## Stack

- Backend: Java 17, Spring Boot 3, Maven, JPA/Hibernate, Spring Security, Flyway, Actuator.
- Banco: PostgreSQL planejado para dev/prod e H2 in-memory no bootstrap/testes.
- Frontend planejado: Vite, React, TypeScript e Tailwind.
- Documentacao de API planejada: Swagger/OpenAPI.
