# Goodfunds — Frontend

SPA do Goodfunds construida com **Vite + React + TypeScript + Tailwind CSS**. Consome a API REST do backend (`../backend`).

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
│   ├── api/             # Cliente HTTP (axios) e chamadas a API
│   ├── components/      # Componentes reutilizaveis de UI
│   ├── pages/           # Telas da aplicacao (uma por rota)
│   ├── hooks/           # Custom hooks React
│   ├── lib/             # Utilitarios e config (env, token de auth)
│   ├── test/            # Setup dos testes (Vitest + Testing Library)
│   ├── App.tsx          # Componente raiz
│   └── main.tsx         # Entry point do Vite
├── index.html
├── tailwind.config.js
├── postcss.config.js
└── vite.config.ts
```

## Comunicacao com a API

O cliente HTTP fica em `src/api/http.ts`:

- **Request interceptor:** injeta `Authorization: Bearer <token>` quando ha JWT salvo (`src/lib/auth-token.ts`).
- **Response interceptor:** em respostas `401`, limpa o token e redireciona para `/login`.

## Testes

Vitest + React Testing Library, ambiente `jsdom`. Arquivos `*.test.ts(x)` ao lado do codigo testado.

```bash
npm run test:run
```
