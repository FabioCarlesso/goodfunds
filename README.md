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

- `docs/context.md`: contexto geral do projeto.
- `docs/context-backend.md`: contexto tecnico do backend.
- `docs/context-frontend.md`: contexto planejado do frontend.
- `docs/goodfunds-planejamento.md`: planejamento do MVP.

## Stack

- Backend: Java 17, Spring Boot 3, Maven, JPA/Hibernate, Spring Security, Flyway, Actuator.
- Banco: PostgreSQL planejado para dev/prod e H2 in-memory no bootstrap/testes.
- Frontend planejado: Vite, React, TypeScript e Tailwind.
- Documentacao de API planejada: Swagger/OpenAPI.
