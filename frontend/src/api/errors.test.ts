import axios from 'axios'
import { describe, expect, it } from 'vitest'
import { getApiErrorMessage } from './errors'

function axiosError(data: unknown, hasResponse = true) {
  return new axios.AxiosError(
    'request failed',
    'ERR_BAD_REQUEST',
    undefined,
    undefined,
    hasResponse
      ? ({ data, status: 400, statusText: '', headers: {}, config: {} } as never)
      : undefined,
  )
}

describe('getApiErrorMessage', () => {
  it('prioriza a primeira mensagem de validacao de campo', () => {
    const error = axiosError({ detail: 'Requisicao invalida', errors: { senha: 'tamanho minimo 8' } })
    expect(getApiErrorMessage(error, 'fallback')).toBe('tamanho minimo 8')
  })

  it('usa o detail do ProblemDetail quando nao ha errors', () => {
    const error = axiosError({ detail: 'Credenciais invalidas' })
    expect(getApiErrorMessage(error, 'fallback')).toBe('Credenciais invalidas')
  })

  it('usa o title quando nao ha detail nem errors', () => {
    const error = axiosError({ title: 'E-mail ja cadastrado' })
    expect(getApiErrorMessage(error, 'fallback')).toBe('E-mail ja cadastrado')
  })

  it('retorna mensagem de rede quando nao ha resposta', () => {
    const error = axiosError(undefined, false)
    expect(getApiErrorMessage(error, 'fallback')).toBe(
      'Nao foi possivel conectar ao servidor. Tente novamente.',
    )
  })

  it('retorna o fallback para erros que nao sao do axios', () => {
    expect(getApiErrorMessage(new Error('boom'), 'fallback')).toBe('fallback')
  })
})
