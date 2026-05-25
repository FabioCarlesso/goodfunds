import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// O jsdom nao implementa ResizeObserver, do qual o recharts (ResponsiveContainer)
// depende. Um stub no-op evita que os graficos quebrem os testes das telas.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = globalThis.ResizeObserver ?? (ResizeObserverStub as unknown as typeof ResizeObserver)

// Desmonta a arvore React e limpa o DOM apos cada teste.
afterEach(() => {
  cleanup()
})
