import { describe, expect, it } from 'vitest'
import { money, parseMoneyToCents } from './format'

describe('money input', () => {
  it('accepts whole amounts and up to two decimal places', () => {
    expect(parseMoneyToCents('24')).toBe(2400)
    expect(parseMoneyToCents('24.5')).toBe(2450)
    expect(parseMoneyToCents('24.50')).toBe(2450)
  })

  it('rejects ambiguous and unsafe values', () => {
    expect(parseMoneyToCents('1.234')).toBeNull()
    expect(parseMoneyToCents('1e3')).toBeNull()
    expect(parseMoneyToCents('-2')).toBeNull()
    expect(parseMoneyToCents('not money')).toBeNull()
  })

  it('formats the ledger currency explicitly', () => {
    expect(money(1234, 'EUR')).toContain('12.34')
  })
})
