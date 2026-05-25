# Goodfunds - Contexto do Frontend

## Estado atual

O scaffold do frontend foi criado em `frontend/` (issue #24): projeto Vite + React + TypeScript com Tailwind configurado, cliente HTTP (Axios) com interceptor `Authorization: Bearer`, estrutura de pastas base e testes com Vitest + React Testing Library. O fluxo de autenticacao foi implementado (issue #25): roteamento com React Router, telas `/login` e `/register` consumindo `/auth/*`, `AuthContext` com persistencia do JWT em `localStorage` e rota protegida via `<RequireAuth>`. As demais telas serao implementadas nas proximas atividades da Sprint 4.

## Stack

- Vite
- React
- TypeScript
- Tailwind CSS
- React Router (roteamento)
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

## Decisoes tomadas

- Estrutura de pastas: `src/api`, `src/components`, `src/contexts`, `src/pages`, `src/hooks`, `src/lib`, `src/types`.
- Cliente HTTP: Axios com interceptors (Bearer no request; redirect para `/login` em 401).
- Estrategia de testes: Vitest + React Testing Library (ambiente `jsdom`).
- Persistencia do JWT: `localStorage` (isolada em `src/lib/auth-token.ts`). Trade-off documentado em `frontend/README.md`: simples para o MVP, porem exposto a XSS; trocar por `sessionStorage`/cookie `httpOnly` exige mudar so esse arquivo.
- Roteamento: React Router em `src/App.tsx`; estado global de auth via `AuthContext`/`useAuth` (issue #25).
- Formularios: controlados nativos com `useState` (sem lib dedicada por ora).

## Decisoes pendentes

- Biblioteca de componentes, se necessaria (ha primitivos proprios em `src/components/ui`).
- Estrategia de cache de dados (React Query / SWR).
- Biblioteca de graficos para relatorios.

## Proximos passos

- Implementar tela Dashboard consumindo `/reports/*`.
- Conectar as demais telas aos endpoints do backend conforme forem entregues.
