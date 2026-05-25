import { http } from './http'
import type { Category } from '../types/category'
import type { TipoCategoria } from '../types/common'

/** `GET /categories` — lista as categorias do usuario, opcionalmente filtrando por tipo. */
export async function listCategories(tipo?: TipoCategoria): Promise<Category[]> {
  const { data } = await http.get<Category[]>('/categories', {
    params: tipo ? { tipo } : undefined,
  })
  return data
}
