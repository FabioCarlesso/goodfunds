import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// Desmonta a arvore React e limpa o DOM apos cada teste.
afterEach(() => {
  cleanup()
})
