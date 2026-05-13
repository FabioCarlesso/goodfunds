# Goodfunds

Goodfunds e um sistema de controle financeiro pessoal para uso proprio. O MVP cobre faturas, transacoes, categorias, planejamento financeiro, relatorios e estimativas.

## Estado atual

O repositorio esta no inicio da Sprint 1. O backend Spring Boot ja foi criado em `backend/`; o frontend, Docker Compose, profiles dedicados e migrations ainda serao implementados.

## Estrutura

```text
goodfunds/
├── backend/                  # Backend Spring Boot 3 + Maven
├── docs/                     # Contextos e planejamento
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
