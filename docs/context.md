# Goodfunds - Contexto Geral

## Objetivo

Goodfunds e um sistema de controle financeiro pessoal para uso proprio. O MVP deve cobrir avaliacao de faturas, planejamento financeiro, estimativas, dashboards e gestao de transacoes e categorias.

## Estrutura do repositorio

```text
goodfunds/
├── backend/                  # Backend Spring Boot
├── docs/                     # Contextos, planejamento e documentacao
├── frontend/                 # Frontend Vite + React + TypeScript + Tailwind (futuro)
└── docker-compose.yml        # Orquestracao local futura
```

No estado atual, apenas o backend foi iniciado.

## Documentos principais

- `docs/context.md`: contexto geral do produto e do repositorio.
- `docs/context-backend.md`: contexto tecnico e operacional do backend.
- `docs/context-frontend.md`: contexto planejado para o frontend.
- `docs/goodfunds-planejamento.md`: planejamento completo do MVP.

## Stack definida

- Backend: Java 17, Spring Boot 3, Maven, Spring Security, JPA/Hibernate, Flyway, Actuator.
- Banco: PostgreSQL para desenvolvimento/producao futura e H2 in-memory para testes/bootstrap.
- API docs: Swagger/OpenAPI via Springdoc.
- PDF: Apache PDFBox para parser de faturas.
- Frontend: Vite, React, TypeScript e Tailwind em sprint futura.
- Testes: JUnit 5 no backend.
- Infra: Docker Compose em sprint futura.

## Estado atual

- Backend criado em `backend/`.
- Maven Wrapper versionado e com checksum da distribuicao Maven.
- Smoke test do contexto Spring passando.
- `application.yml` base usa H2 temporario para permitir execucao local sem PostgreSQL.
- Flyway ainda desabilitado ate a primeira migration.
- Frontend, Docker Compose e profiles dedicados ainda nao foram criados.

## Proximos passos gerais

- Criar profiles `dev`, `test` e `prod` para o backend.
- Criar migrations Flyway iniciais.
- Implementar entidades, repositories, services e controllers.
- Configurar autenticacao JWT.
- Criar frontend quando a sprint correspondente iniciar.
- Adicionar Docker Compose para execucao end-to-end.
