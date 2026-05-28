# Goodfunds

Goodfunds e um sistema de controle financeiro pessoal para uso proprio. O MVP cobre faturas, transacoes, categorias, planejamento financeiro, relatorios e estimativas.

## Sumario

- [Visao geral](#visao-geral)
- [Stack](#stack)
- [Arquitetura](#arquitetura)
- [Pre-requisitos](#pre-requisitos)
- [Estrutura do repositorio](#estrutura-do-repositorio)
- [Executar com Docker Compose](#executar-com-docker-compose)
- [Executar backend manualmente](#executar-backend-manualmente)
- [Executar frontend manualmente](#executar-frontend-manualmente)
- [Executar testes](#executar-testes)
- [Swagger e Actuator](#swagger-e-actuator)
- [Documentacao](#documentacao)

## Visao geral

O Goodfunds e um app pessoal para registrar receitas e despesas, importar faturas de cartao de credito (PDF do Nubank no MVP), categorizar transacoes, definir orcamentos por categoria e visualizar relatorios mensais e por categoria. A solucao e composta por:

- **Backend** Spring Boot 3 expondo API REST autenticada por JWT, com persistencia em PostgreSQL (e H2 in-memory nos testes).
- **Frontend** SPA em Vite + React + TypeScript + Tailwind, com telas de Dashboard, Faturas, Planejamento e Relatorios.
- **Infra local** orquestrada por Docker Compose (backend + Postgres + frontend).

## Stack

- **Backend:** Java 17, Spring Boot 3, Maven, Spring Security, JPA/Hibernate, Flyway, Actuator, Springdoc OpenAPI, Caffeine (cache de relatorios), JUnit 5 + Mockito.
- **Banco:** PostgreSQL 16 (dev/prod) e H2 in-memory (testes).
- **Frontend:** Vite, React, TypeScript, Tailwind CSS, React Router, Axios, Recharts; testes com Vitest + React Testing Library.
- **Infra:** Docker, Docker Compose, nginx (serve o build do frontend).

## Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│                        FRONTEND                         │
│         Vite + React + TypeScript + Tailwind            │
│                                                         │
│  Dashboard │ Faturas │ Planejamento │ Relatorios │ Login │
└─────────────────────────┬───────────────────────────────┘
                          │ REST / JSON (JWT no header)
┌─────────────────────────▼───────────────────────────────┐
│                        BACKEND                          │
│              Java 17 + Spring Boot 3 + Maven            │
│                                                         │
│  Auth API  │ Fatura API │ Finance API │ Report API │ Docs│
│  (JWT)     │ (PDF/Parse)│ (Transacoes)│ (Projecoes)│ (SW)│
└─────────────────────────┬───────────────────────────────┘
                          │ JPA / Hibernate (Flyway)
┌─────────────────────────▼───────────────────────────────┐
│                     BANCO DE DADOS                      │
│         PostgreSQL (dev/prod) · H2 in-memory (testes)   │
│                                                         │
│  User │ Invoice │ Transaction │ Category │ Budget       │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                    INFRA & TOOLING                      │
│  Docker │ Git │ JUnit 5 │ Caffeine │ Actuator │ Swagger │
└─────────────────────────────────────────────────────────┘
```

Diagrama detalhado e fluxos completos em `docs/goodfunds-planejamento.md` e `docs/doc.md`.

## Pre-requisitos

Para rodar **somente com Docker Compose** (modo recomendado):

- Docker Engine 24+ e Docker Compose v2.

Para rodar **componentes manualmente** (sem Docker):

- **Backend:** Java 17+, terminal compativel com o Maven Wrapper (`./mvnw`). Para usar PostgreSQL, uma instancia local em `localhost:5432`.
- **Frontend:** Node.js 20+ e npm 10+.

## Estrutura do repositorio

```text
goodfunds/
├── backend/                  # Backend Spring Boot 3 + Maven (com Dockerfile)
├── frontend/                 # Frontend Vite + React + TypeScript + Tailwind (Dockerfile + nginx)
├── docs/                     # Documentacao tecnica e planejamento
├── docker-compose.yml        # Stack local: backend + postgres + frontend
├── .env.example              # Template das variaveis de ambiente do compose
└── README.md
```

## Executar com Docker Compose

Stack local completa: backend Spring Boot + PostgreSQL + frontend (Vite build servido por nginx).

```bash
cp .env.example .env
# Edite .env e defina JWT_SECRET com um valor longo e aleatorio (ex.: openssl rand -base64 48).
# Opcional: ajuste FRONTEND_PORT, APP_PORT, POSTGRES_PORT, APP_UPLOADS_DIR.
docker compose up --build
```

Servicos expostos:

| Servico | URL | Variavel de porta |
|---|---|---|
| Frontend | `http://localhost:3000` | `FRONTEND_PORT` |
| Backend (API) | `http://localhost:8080` | `APP_PORT` |
| PostgreSQL | `localhost:5432` | `POSTGRES_PORT` |

Os dados do banco persistem no volume nomeado `goodfunds-postgres-data`.

> **CORS / `VITE_API_BASE_URL`:** o frontend roda no browser e chama a API direto no host. Por isso, `VITE_API_BASE_URL` (assada na imagem em build time) deve apontar para a origem onde o backend e exposto (default `http://localhost:8080`). A origem do frontend (`http://localhost:3000`) ja vem liberada em `APP_CORS_ALLOWED_ORIGINS`. Se mudar `FRONTEND_PORT` ou `APP_PORT`, atualize ambos.

Validacao rapida (apos `docker compose up`):

```bash
curl http://localhost:8080/actuator/health
# abra http://localhost:3000 no browser e cadastre/login com um usuario novo
```

Derrubar:

```bash
docker compose down            # para os containers, mantem o volume
docker compose down -v         # tambem apaga os dados do Postgres
```

## Executar backend manualmente

Sem Docker, com o profile `dev` (default — usa H2 in-memory):

```bash
cd backend
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Detalhes de perfis, variaveis e endpoints em `docs/doc-backend.md`.

Para apontar para um PostgreSQL local, defina as variaveis `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` e `JWT_SECRET` e ative o perfil `prod`:

```bash
SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/goodfunds \
SPRING_DATASOURCE_USERNAME=goodfunds \
SPRING_DATASOURCE_PASSWORD=goodfunds \
JWT_SECRET="$(openssl rand -base64 48)" \
./mvnw spring-boot:run
```

## Executar frontend manualmente

```bash
cd frontend
npm install
cp .env.example .env.local   # ajuste VITE_API_BASE_URL se a API nao estiver em :8080
npm run dev
```

O dev server sobe em `http://localhost:5173`. Detalhes de scripts, rotas, variaveis de ambiente e estrutura em `frontend/README.md`.

## Executar testes

**Backend** (JUnit 5 + Mockito, com cobertura JaCoCo):

```bash
cd backend
./mvnw verify
```

Os relatorios de cobertura ficam em `backend/target/site/jacoco/`.

**Frontend** (Vitest + React Testing Library):

```bash
cd frontend
npm run test:run     # execucao unica (CI)
npm run test         # modo watch
npm run lint         # ESLint
npm run build        # type-check + build de producao
```

## Swagger e Actuator

Com o backend rodando (manual ou via compose):

| Recurso | URL | Observacoes |
|---|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` | Habilitado em `dev`/`test`; desativado em `prod`. |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` | Spec OpenAPI 3 do Springdoc. |
| Health check | `http://localhost:8080/actuator/health` | Exposto em todos os perfis. |
| Info | `http://localhost:8080/actuator/info` | Exposto quando habilitado em `application.yml`. |

## Documentacao

Documentacao tecnica principal:

- `docs/doc.md`: visao geral do sistema, arquitetura, modulos e fluxos principais.
- `docs/doc-backend.md`: documentacao tecnica do backend (entidades, endpoints, perfis, seguranca).
- `docs/doc-frontend.md`: documentacao do frontend (telas, navegacao, integracao com a API).
- `docs/index.html`: documentacao visual do sistema — abrir direto no navegador.

Documentos de contexto e planejamento:

- `docs/context.md`: contexto geral do produto e do repositorio.
- `docs/context-backend.md`: contexto operacional do backend.
- `docs/context-frontend.md`: contexto planejado do frontend.
- `docs/goodfunds-planejamento.md`: planejamento completo do MVP.
