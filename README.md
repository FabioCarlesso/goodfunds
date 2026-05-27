# Goodfunds

Goodfunds e um sistema de controle financeiro pessoal para uso proprio. O MVP cobre faturas, transacoes, categorias, planejamento financeiro, relatorios e estimativas.

## Estado atual

O repositorio esta na Sprint 1. O backend Spring Boot ja foi criado em `backend/`, com profiles `dev`, `test` e `prod`, migrations Flyway, autenticacao JWT, CRUDs de categorias/transacoes/orcamentos e upload autenticado de faturas PDF. O Docker Compose basico (app + postgres) ja esta disponivel (issue #11) e agora inclui tambem o frontend (issue #28). O scaffold do frontend (Vite + React + TypeScript + Tailwind) ja foi criado em `frontend/` (issue #24), o fluxo de autenticacao (telas de login/cadastro, roteamento e persistencia do JWT) foi implementado (issue #25) e as telas MVP (Dashboard, Faturas, Planejamento e Relatorios, com navegacao por menu lateral) foram implementadas (issue #26).

## Estrutura

```text
goodfunds/
├── backend/                  # Backend Spring Boot 3 + Maven (com Dockerfile)
├── frontend/                 # Frontend Vite + React + TypeScript + Tailwind (com Dockerfile + nginx)
├── docs/                     # Contextos e planejamento
├── docker-compose.yml        # Stack local: app + postgres + frontend
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

## Frontend

SPA em Vite + React + TypeScript + Tailwind em `frontend/`. Consome a API REST do backend.

Requisitos:

- Node.js 20+ e npm 10+

Executar:

```bash
cd frontend
npm install
npm run dev
```

O dev server sobe em `http://localhost:5173`. Detalhes de scripts, variaveis de ambiente e estrutura em `frontend/README.md`.

## Docker Compose

Stack local completa: backend Spring Boot + PostgreSQL + frontend (Vite build servido por nginx).

Requisitos:

- Docker Engine 24+ e Docker Compose v2.

Passos:

```bash
cp .env.example .env
# edite .env e defina JWT_SECRET com um valor longo e aleatorio
# opcional: ajuste FRONTEND_PORT, APP_PORT, APP_UPLOADS_DIR
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

- Backend: Java 17, Spring Boot 3, Maven, JPA/Hibernate, Spring Security, Flyway, Actuator, Caffeine (cache de relatorios).
- Banco: PostgreSQL planejado para dev/prod e H2 in-memory no bootstrap/testes.
- Frontend: Vite, React, TypeScript, Tailwind CSS, React Router, Axios; testes com Vitest + React Testing Library.
- Documentacao de API: Swagger/OpenAPI 3 via Springdoc. Swagger UI em `http://localhost:8080/swagger-ui.html` (habilitado em dev; desativado em prod).
