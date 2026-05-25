import { describe, expect, it } from 'vitest'
import {
  addMonths,
  currentRef,
  formatCurrency,
  formatDate,
  formatMonthLabel,
  refToParts,
} from './format'

describe('format', () => {
  it('formata valores em Reais', () => {
    const result = formatCurrency(1234.56)
    expect(result).toContain('R$')
    expect(result).toMatch(/1\.234,56/)
  })

  it('trata null/undefined como zero na formatacao de moeda', () => {
    expect(formatCurrency(null)).toMatch(/0,00/)
    expect(formatCurrency(undefined)).toMatch(/0,00/)
  })

  it('soma e subtrai meses cruzando a virada de ano', () => {
    expect(addMonths('2026-05', 1)).toBe('2026-06')
    expect(addMonths('2026-01', -1)).toBe('2025-12')
    expect(addMonths('2026-12', 1)).toBe('2027-01')
  })

  it('decompoe o ref em mes e ano 1-based', () => {
    expect(refToParts('2026-05')).toEqual({ mes: 5, ano: 2026 })
  })

  it('produz o ref corrente no formato YYYY-MM', () => {
    expect(currentRef()).toMatch(/^\d{4}-\d{2}$/)
  })

  it('formata o ref como rotulo legivel', () => {
    expect(formatMonthLabel('2026-05')).toMatch(/2026/)
  })

  it('formata data ISO no padrao brasileiro', () => {
    expect(formatDate('2026-05-09')).toBe('09/05/2026')
  })
})
