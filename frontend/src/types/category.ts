import type { TipoCategoria } from './common'

/** Categoria do usuario (`GET /categories`). */
export interface Category {
  id: string
  nome: string
  tipo: TipoCategoria
}

/** Corpo de criacao/edicao de categoria (`POST`/`PUT /categories`). */
export interface CategoryRequest {
  nome: string
  tipo: TipoCategoria
}
