export function money(cents: number, currency = 'USD') {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(cents / 100)
}

export function shortDate(value: string | null) {
  if (!value) return 'Pending'
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(new Date(`${value}T12:00:00`))
}

export function initials(name: string) {
  return name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()
}

export function parseMoneyToCents(value: string) {
  const normalized = value.trim()
  if (!/^\d+(?:\.\d{1,2})?$/.test(normalized)) return null
  const [whole, fraction = ''] = normalized.split('.')
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
  return Number.isSafeInteger(cents) ? cents : null
}
