# Goodfunds — Documentação Técnica do Frontend

## Estado atual

O **scaffold** do frontend foi criado em `frontend/` (issue #24): projeto Vite + React + TypeScript com Tailwind configurado, cliente HTTP (Axios) com interceptor `Authorization: Bearer`, estrutura de pastas base (`src/api`, `src/components`, `src/pages`, `src/hooks`, `src/lib`) e testes com Vitest + React Testing Library.

O **fluxo de autenticação** foi implementado (issue #25): roteamento com React Router em `src/App.tsx`, telas `/login` e `/register` (`src/pages/`) consumindo `/auth/*`, `AuthContext` (`src/contexts/`) com persistência do JWT em `localStorage` via `useAuth()`, rota protegida por `<RequireAuth>` (`src/components/`) que redireciona para `/login`, e logout que limpa o token.

As **telas MVP** foram implementadas (issue #26): Dashboard (`/dashboard`), Faturas (`/faturas`) com upload e detalhe (`/faturas/:id`), Planejamento (`/planejamento`) e Relatórios (`/relatorios`). As rotas protegidas compartilham o `AppLayout` (`src/components/layout/`) com **menu lateral** de navegação; os gráficos usam **Recharts**. As rotas seguem nomes em português conforme os critérios de aceite da issue #26. A tela de transações (CRUD manual) e o serviço `frontend` no Docker Compose seguem como próximos passos.

> **Pendências de backend:** as telas de Faturas consomem `GET /invoices` e `GET /invoices/{id}` (lista e detalhe com transações geradas), previstos em `goodfunds-planejamento.md` mas ainda não expostos pelo backend (hoje só existe `POST /invoices/upload`). O recurso de orçamentos não possui `DELETE`, então o Planejamento cobre criação e edição.

---

## Stack planejada

| Tecnologia | Finalidade |
|---|---|
| **Vite** | Bundler e dev server com hot reload rápido |
| **React** | Biblioteca de UI baseada em componentes |
| **TypeScript** | Tipagem estática para segurança e produtividade |
| **Tailwind CSS** | Estilização utilitária sem CSS separado |

---

## Estrutura de pastas planejada

```
frontend/
├── public/               # Arquivos estáticos públicos
├── src/
│   ├── assets/           # Imagens, ícones, fontes
│   ├── components/       # Componentes reutilizáveis
│   │   ├── ui/           # Elementos base (Button, Input, Card, Modal)
│   │   ├── layout/       # Header, Sidebar, PageWrapper
│   │   └── charts/       # Gráficos de relatórios
│   ├── pages/            # Telas da aplicação (uma por rota)
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   ├── Dashboard.tsx
│   │   ├── Transactions.tsx
│   │   ├── Invoices.tsx
│   │   ├── Planning.tsx
│   │   └── Reports.tsx
│   ├── hooks/            # Custom hooks (useAuth, useTransactions, etc.)
│   ├── services/         # Clientes HTTP para cada recurso da API
│   ├── contexts/         # Context API para estado global (auth, user)
│   ├── types/            # Interfaces e tipos TypeScript
│   ├── utils/            # Funções auxiliares (formatação, datas, moeda)
│   ├── routes/           # Configuração de rotas (React Router)
│   ├── App.tsx           # Componente raiz
│   └── main.tsx          # Entry point Vite
├── index.html
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.ts
└── package.json
```

---

## Telas principais

### Login (`/login`) — implementada (issue #25)

- Formulário controlado com e-mail e senha.
- Chamada: `POST /auth/login`.
- Em caso de sucesso: armazena o JWT (via `AuthContext`) e redireciona para a rota de origem (ou Dashboard).
- Exibe mensagem de erro em credenciais inválidas (traduzida do `ProblemDetail` do backend).

### Cadastro (`/register`) — implementada (issue #25)

- Formulário controlado com nome, e-mail e senha (senha mínima de 8 caracteres, alinhada ao backend).
- Chamada: `POST /auth/register`.
- Cria conta e redireciona para `/login` com mensagem de sucesso.

### Dashboard (`/`)

- Visão consolidada do mês corrente.
- Exibe: saldo, total de receitas, total de despesas, progresso do orçamento por categoria.
- Fonte: `GET /reports/summary?ref=YYYY-MM`.
- Gráfico de pizza ou barras com gastos por categoria.
- Fonte: `GET /reports/by-category?ref=YYYY-MM`.
- Seletor de mês/ano para navegar entre períodos.

### Transações (`/transactions`)

- Tabela paginada de lançamentos com filtros por mês, categoria e tipo (RECEITA/DESPESA).
- Fonte: `GET /transactions`.
- Ações: criar, editar e excluir transações.
- Formulário de criação/edição com campos: descrição, valor, data, categoria, forma de pagamento.

### Faturas (`/invoices`)

- Lista de faturas importadas com status (`PENDENTE_PARSE`, `PROCESSADA`, `ERRO`).
- Upload de nova fatura: input de arquivo PDF com drag-and-drop.
- Chamada: `POST /invoices/upload`.
- Detalhe da fatura: lista de transações geradas com opção de reclassificar categoria.

### Planejamento (`/planning`)

- Lista de orçamentos mensais por categoria.
- Criação e edição de limites por categoria/mês.
- Indicador visual de progresso: gasto atual vs. limite definido.
- Fonte: `GET /budgets?ref=YYYY-MM`.

### Relatórios (`/reports`)

- Gráfico de evolução mensal de receitas e despesas.
- Fonte: `GET /reports/evolution?from=YYYY-MM&to=YYYY-MM`.
- Projeção do mês atual com base na média dos últimos 3 meses.
- Fonte: `GET /reports/estimate`.
- Filtros de período.

---

## Fluxo de navegação

```
/login  ──(autenticado)──►  /  (Dashboard)
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
  /transactions          /invoices           /planning
                              │
                              ▼
                         /invoices/:id
                              │
         ┌────────────────────┘
         ▼
     /reports

Rota sem autenticação → redireciona para /login
```

---

## Comunicação com a API

### Autenticação

- O JWT retornado pelo login é armazenado em `localStorage` (decisão MVP — ver trade-off em `frontend/README.md`), isolado em `src/lib/auth-token.ts`.
- Todas as requisições autenticadas incluem o header: `Authorization: Bearer <token>`.
- Em caso de resposta 401, o token é limpo e o usuário é redirecionado para `/login`.

### Cliente HTTP

- Biblioteca a definir na sprint (Axios ou Fetch nativo com wrapper).
- Interceptor global para injetar o header `Authorization` em todas as chamadas.
- Interceptor de resposta para tratar erros 401 e 422.

### Paginação

- Endpoints de listagem retornam `Page<T>` com campos: `content`, `totalElements`, `totalPages`, `number`, `size`.
- Parâmetros de paginação: `?page=0&size=20&sort=data,desc`.

### Tipagem

- Todos os contratos de resposta da API devem ser tipados como interfaces TypeScript em `src/types/`.
- Exemplo: `Transaction`, `Category`, `Invoice`, `Budget`, `ReportSummary`.

---

## Gerenciamento de estado

- **Autenticação:** Context API com `AuthContext` provendo `user`, `token`, `login()`, `logout()`.
- **Estado de página:** estado local com `useState` e `useReducer` por tela.
- **Cache de dados:** a definir (React Query / SWR são candidatos para gerenciar fetching, cache e revalidação).

---

## Padrões visuais e comportamentais

- Tailwind CSS como única fonte de estilos; sem CSS separado.
- Paleta de cores e tipografia definidas no `tailwind.config.ts`.
- Componentes base reutilizáveis em `src/components/ui/`: `Button`, `Input`, `Select`, `Card`, `Modal`, `Badge`, `Spinner`.
- Feedback visual em todas as ações assíncronas: loading spinner durante fetch, toast de sucesso/erro após mutations.
- Formulários com validação client-side antes do envio à API.
- Interface responsiva para desktop e mobile.

---

## Decisões tomadas

| Decisão | Escolha |
|---|---|
| Cliente HTTP | Axios com interceptors (`src/api/http.ts`) |
| Persistência do JWT | `localStorage` isolado em `src/lib/auth-token.ts` (trade-off em `frontend/README.md`) |
| Roteamento | React Router (`src/App.tsx`) |
| Estado global de auth | Context API (`AuthContext` / `useAuth`) |
| Estratégia de testes | Vitest + React Testing Library (`jsdom`) |
| Gerenciamento de formulários | Formulários controlados nativos (`useState`) |
| Biblioteca de componentes | Tailwind + primitivos próprios (`src/components/ui`) |

## Decisões pendentes

| Decisão | Opções consideradas |
|---|---|
| Gerenciamento de dados assíncronos | React Query (TanStack Query), SWR |
| Biblioteca de gráficos | Recharts, Chart.js, Nivo |

---

## Próximos passos

1. ~~Criar scaffold Vite + React + TypeScript + Tailwind em `frontend/`.~~ (concluído — issue #24)
2. ~~Configurar React Router e estrutura de rotas.~~ (concluído — issue #25)
3. ~~Implementar `AuthContext` e tela de Login/Cadastro consumindo `/auth/*`.~~ (concluído — issue #25)
4. ~~Implementar tela Dashboard consumindo `/reports/summary`.~~ (concluído — issue #26)
5. ~~Implementar upload e listagem de faturas consumindo `/invoices/*`.~~ (concluído — issue #26; lista/detalhe dependem dos endpoints `GET /invoices` e `GET /invoices/{id}` no backend)
6. ~~Implementar planejamento consumindo `/budgets`.~~ (concluído — issue #26)
7. ~~Implementar relatórios consumindo `/reports/evolution` e `/reports/by-category`.~~ (concluído — issue #26)
8. Implementar CRUD de transações consumindo `/transactions` e `/categories`.
9. Expor no backend `GET /invoices` e `GET /invoices/{id}` para a listagem e o detalhe de faturas.
10. Configurar Docker Compose com serviço `frontend` (build Vite + Nginx).
