# Goodfunds - Contexto do Frontend

## Estado atual

O frontend ainda nao foi criado neste repositorio. Ele esta planejado para uma sprint futura, depois da base do backend e dos endpoints principais.

## Stack planejada

- Vite
- React
- TypeScript
- Tailwind

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

## Decisoes pendentes

- Estrutura final de pastas.
- Biblioteca de componentes, se necessaria.
- Cliente HTTP e estrategia de cache.
- Padrao de formularios e validacao client-side.
- Estrategia de testes do frontend.

## Proximos passos

- Criar o scaffold em `frontend/`.
- Definir layout base, rotas e gerenciamento de estado.
- Implementar fluxo de login/cadastro apos a API de auth.
- Conectar telas aos endpoints do backend conforme forem entregues.
