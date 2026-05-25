# Goodfunds — Frontend

SPA do Goodfunds construida com **Vite + React + TypeScript + Tailwind CSS**, com roteamento via **React Router**. Consome a API REST do backend (`../backend`).

## Requisitos

- Node.js 20+ e npm 10+

## Setup

```bash
cd frontend
npm install
cp .env.example .env.local   # ajuste VITE_API_BASE_URL se necessario
npm run dev                  # sobe em http://localhost:5173
```

## Scripts

| Comando | Descricao |
|---|---|
| `npm run dev` | Dev server com HMR em `:5173` |
| `npm run build` | Type-check (`tsc -b`) + build de producao em `dist/` |
| `npm run preview` | Serve o build de producao localmente |
| `npm run lint` | ESLint |
| `npm run test` | Testes (Vitest) em modo watch |
| `npm run test:run` | Testes (Vitest) em modo single-run |

## Variaveis de ambiente

Apenas variaveis prefixadas com `VITE_` ficam expostas no cliente. Veja `.env.example`.

| Variavel | Default | Descricao |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | URL base da API do backend |

## Estrutura

```
frontend/
├── public/              # Arquivos estaticos servidos como estao
├── src/
│   ├── api/             # Cliente HTTP (axios), chamadas a API e parsing de erros
│   ├── components/      # Componentes reutilizaveis (RequireAuth, ui/)
│   ├── contexts/        # Context API (AuthProvider / AuthContext)
│   ├── pages/           # Telas da aplicacao (uma por rota)
│   ├── hooks/           # Custom hooks React (useAuth, useAuthToken)
│   ├── lib/             # Utilitarios e config (env, token de auth)
│   ├── types/           # Interfaces dos contratos da API
│   ├── test/            # Setup dos testes (Vitest + Testing Library)
│   ├── App.tsx          # Componente raiz: providers + rotas
│   └── main.tsx         # Entry point do Vite
├── index.html
├── tailwind.config.js
├── postcss.config.js
└── vite.config.ts
```

## Rotas

O roteamento (React Router) fica em `src/App.tsx`:

| Rota | Acesso | Tela |
|---|---|---|
| `/login` | Publica (redireciona p/ `/` se autenticado) | `LoginPage` |
| `/register` | Publica (redireciona p/ `/` se autenticado) | `RegisterPage` |
| `/` | Protegida (`<RequireAuth>`) | `HomePage` (placeholder do Dashboard) |
| `*` | — | redireciona para `/` |

`<RequireAuth>` (`src/components/RequireAuth.tsx`) bloqueia rotas autenticadas: sem JWT, redireciona para `/login` guardando a rota de origem para retorno apos o login.

## Autenticacao

- Telas `LoginPage` (`/login`) e `RegisterPage` (`/register`) consomem `POST /auth/login` e `POST /auth/register` (`src/api/auth.ts`).
- O estado global de auth vive no `AuthProvider` (`src/contexts/`), consumido via `useAuth()`. `login(token)` persiste o JWT, `logout()` o limpa.
- Apos o cadastro, o usuario e redirecionado para `/login` com mensagem de sucesso; apos o login, vai para a rota de origem (ou `/`).

### Persistencia do JWT (decisao MVP)

O JWT e armazenado em **`localStorage`** (`src/lib/auth-token.ts`), isolado atras de `getToken/setToken/clearToken`.

- **Por que:** simplicidade no MVP de uso pessoal — sobrevive a recarregar a pagina e nao exige cookies/CSRF nem backend de sessao.
- **Trade-off:** `localStorage` e acessivel por JavaScript, logo vulneravel a roubo de token via XSS (um cookie `httpOnly` seria mais resistente). Mitiga-se mantendo dependencias atualizadas e evitando renderizar HTML nao confiavel. Trocar a estrategia (sessionStorage ou cookie `httpOnly`) exige mudar apenas `auth-token.ts`.

## Comunicacao com a API

O cliente HTTP fica em `src/api/http.ts`:

- **Request interceptor:** injeta `Authorization: Bearer <token>` quando ha JWT salvo (`src/lib/auth-token.ts`).
- **Response interceptor:** em respostas `401`, limpa o token e redireciona para `/login`.
- **Erros:** `src/api/errors.ts` traduz o `ProblemDetail` (RFC 7807) do backend em mensagem amigavel para as telas.

> **CORS:** o frontend (`:5173`) e o backend (`:8080`) ficam em origens distintas. O backend libera `http://localhost:5173` por padrao (`APP_CORS_ALLOWED_ORIGINS`). O dev server usa `strictPort` (sempre `:5173`) para a origem casar com o CORS — se a `5173` estiver ocupada, o `npm run dev` falha em vez de pular para `5174` (o que geraria erro de CORS por origem nao permitida). Se o login retornar "Nao foi possivel conectar ao servidor" com o backend no ar, confirme que o frontend esta em `:5173` e que essa origem esta em `APP_CORS_ALLOWED_ORIGINS`.

## Testes

Vitest + React Testing Library, ambiente `jsdom`. Arquivos `*.test.ts(x)` ao lado do codigo testado.

```bash
npm run test:run
```
