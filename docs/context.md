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
- Infra: Docker Compose em sprint futura.

## Estado atual

- Backend criado em `backend/`.
- Maven Wrapper versionado e com checksum da distribuicao Maven.
- Smoke test do contexto Spring passando.
- Perfis `dev`, `test` e `prod` configurados em `application.yml` (issue #3).
- Perfil `dev`: PostgreSQL local (porta 5432). Perfil `test`: H2 in-memory. Perfil `prod`: variaveis de ambiente.
- Flyway desabilitado em `dev` e `test` ate a criacao da primeira migration (issue #4).
- Estrutura de pacotes `com.goodfunds` criada com `package-info.java` em cada pacote (issue #2).
- Documentacao tecnica criada em `docs/` (issue #35).
- Frontend e Docker Compose ainda nao foram criados.

## Proximos passos gerais

- Criar migrations Flyway iniciais (issue #4).
- Implementar entidades, repositories, services e controllers.
- Configurar autenticacao JWT.
- Criar frontend quando a sprint correspondente iniciar.
- Adicionar Docker Compose para execucao end-to-end.
