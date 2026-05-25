# Goodfunds - Contexto do Frontend

## Estado atual

O scaffold do frontend foi criado em `frontend/` (issue #24): projeto Vite + React + TypeScript com Tailwind configurado, cliente HTTP (Axios) com interceptor `Authorization: Bearer`, estrutura de pastas base e testes com Vitest + React Testing Library. O fluxo de autenticacao foi implementado (issue #25): roteamento com React Router, telas `/login` e `/register` consumindo `/auth/*`, `AuthContext` com persistencia do JWT em `localStorage` e rota protegida via `<RequireAuth>`. As telas MVP foram implementadas (issue #26): Dashboard, Faturas (lista/upload/detalhe), Planejamento (orcamentos) e Relatorios (graficos com Recharts), com navegacao por menu lateral (`AppLayout`).

## Stack

- Vite
- React
- TypeScript
- Tailwind CSS
- React Router (roteamento)
- Recharts (graficos)
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
- Roteamento: React Router em `src/App.tsx`; estado global de auth via `AuthContext`/`useAuth` (issue #25). Telas protegidas compartilham o `AppLayout` (menu lateral) em `src/components/layout`.
- Formularios: controlados nativos com `useState` (sem lib dedicada por ora).
- Biblioteca de graficos: Recharts (issue #26), usada no Dashboard e nos Relatorios.
- Carregamento de dados: `useEffect` por tela com estados de loading/erro/vazio; sem lib de cache por ora.

## Decisoes pendentes

- Biblioteca de componentes, se necessaria (ha primitivos proprios em `src/components/ui`).
- Estrategia de cache de dados (React Query / SWR).
- Tela de transacoes (CRUD manual) ainda nao implementada.

## Proximos passos

- Implementar a tela de transacoes (CRUD manual) consumindo `/transactions` e `/categories`.
- Implementar no backend `GET /invoices` e `GET /invoices/{id}` (lista e detalhe), hoje consumidos pelo frontend mas ainda nao expostos.
- Configurar o servico `frontend` no Docker Compose (build Vite + Nginx).
