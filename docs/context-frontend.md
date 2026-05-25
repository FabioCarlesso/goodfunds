# Goodfunds - Contexto do Frontend

## Estado atual

O scaffold do frontend foi criado em `frontend/` (issue #24): projeto Vite + React + TypeScript com Tailwind configurado, cliente HTTP (Axios) com interceptor `Authorization: Bearer`, estrutura de pastas base e testes com Vitest + React Testing Library. As telas ainda serao implementadas nas proximas atividades da Sprint 4.

## Stack

- Vite
- React
- TypeScript
- Tailwind CSS
- Axios (cliente HTTP)
- Vitest + React Testing Library (testes)

## Objetivo do frontend

O frontend deve entregar a experiencia MVP para uso pessoal do Goodfunds, consumindo a API REST do backend.

## Telas previstas

- Login e cadastro.
- Dashboard financeiro.
- Listagem, criacao e edicao de transacoes.
- Importacao e acompanhamento de faturas.
- Planejamento por orcamentos mensais.
- Relatorios e estimativas.

## Integracao com backend

- Comunicacao via REST/JSON.
- Autenticacao por JWT quando a API de auth estiver pronta.
- Persistencia do token no cliente conforme decisao da sprint de frontend.
- Consumo de endpoints paginados para listagens.

## Decisoes tomadas no scaffold

- Estrutura de pastas: `src/api`, `src/components`, `src/pages`, `src/hooks`, `src/lib`.
- Cliente HTTP: Axios com interceptors (Bearer no request; redirect para `/login` em 401).
- Estrategia de testes: Vitest + React Testing Library (ambiente `jsdom`).
- Persistencia do JWT: `localStorage` (isolada em `src/lib/auth-token.ts`).

## Decisoes pendentes

- Biblioteca de componentes, se necessaria.
- Roteamento (React Router) e gerenciamento de estado global.
- Estrategia de cache de dados (React Query / SWR).
- Padrao de formularios e validacao client-side.
- Biblioteca de graficos para relatorios.

## Proximos passos

- Configurar roteamento e layout base.
- Implementar fluxo de login/cadastro consumindo a API de auth.
- Conectar telas aos endpoints do backend conforme forem entregues.
