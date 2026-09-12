import type {
  ApiError,
  AuthResponse,
  Balance,
  BankConnection,
  BankTransaction,
  Expense,
  Group,
  MemberRole,
  SplitInput,
  SplitStrategy,
  Transfer,
  User,
} from './types'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiClientError extends Error {
  constructor(public readonly response: ApiError) {
    super(response.message)
  }
}

class ApiClient {
  private token: string | null = null

  setToken(token: string | null) {
    this.token = token
  }

  register(email: string, displayName: string, password: string) {
    return this.request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, displayName, password }),
    })
  }

  login(email: string, password: string) {
    return this.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
  }

  me() {
    return this.request<User>('/users/me')
  }

  groups() {
    return this.request<Group[]>('/groups')
  }

  group(groupId: string) {
    return this.request<Group>(`/groups/${groupId}`)
  }

  createGroup(name: string) {
    return this.request<Group>('/groups', { method: 'POST', body: JSON.stringify({ name }) })
  }

  addMember(groupId: string, email: string, role: MemberRole) {
    return this.request<Group>(`/groups/${groupId}/members`, {
      method: 'POST',
      body: JSON.stringify({ email, role }),
    })
  }

  removeMember(groupId: string, userId: string) {
    return this.request<void>(`/groups/${groupId}/members/${userId}`, { method: 'DELETE' })
  }

  expenses(groupId: string) {
    return this.request<Expense[]>(`/groups/${groupId}/expenses`)
  }

  createExpense(input: {
    groupId: string
    description: string
    amountCents: number
    payerId: string
    splitStrategy: SplitStrategy
    splits: SplitInput[]
  }) {
    const { groupId, ...body } = input
    return this.request<Expense>(`/groups/${groupId}/expenses`, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  }

  deleteExpense(groupId: string, expenseId: string) {
    return this.request<void>(`/groups/${groupId}/expenses/${expenseId}`, { method: 'DELETE' })
  }

  balances(groupId: string) {
    return this.request<Balance[]>(`/groups/${groupId}/balances`)
  }

  settlementPlan(groupId: string) {
    return this.request<Transfer[]>(`/groups/${groupId}/settle-up`)
  }

  recordSettlement(groupId: string, fromUserId: string, toUserId: string, amountCents: number) {
    return this.request(`/groups/${groupId}/settlements`, {
      method: 'POST',
      body: JSON.stringify({ fromUserId, toUserId, amountCents }),
    })
  }

  createLinkToken() {
    return this.request<{ linkToken: string; expiration: string }>('/bank/link-token', { method: 'POST' })
  }

  exchangePublicToken(publicToken: string, institutionId?: string, institutionName?: string) {
    return this.request<BankConnection>('/bank/connections', {
      method: 'POST',
      body: JSON.stringify({ publicToken, institutionId, institutionName }),
    })
  }

  bankConnections() {
    return this.request<BankConnection[]>('/bank/connections')
  }

  syncBankConnection(connectionId: string) {
    return this.request<{ added: number; modified: number; removed: number; syncedAt: string }>(
      `/bank/connections/${connectionId}/sync`,
      { method: 'POST' },
    )
  }

  disconnectBank(connectionId: string) {
    return this.request<void>(`/bank/connections/${connectionId}`, { method: 'DELETE' })
  }

  bankTransactions() {
    return this.request<BankTransaction[]>('/bank/transactions')
  }

  importTransaction(
    transactionId: string,
    input: { groupId: string; description?: string; splitStrategy: SplitStrategy; splits: SplitInput[] },
  ) {
    return this.request<Expense>(`/bank/transactions/${transactionId}/import`, {
      method: 'POST',
      body: JSON.stringify(input),
    })
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers)
    if (init.body) headers.set('Content-Type', 'application/json')
    if (this.token) headers.set('Authorization', `Bearer ${this.token}`)
    const response = await fetch(`${API_URL}${path}`, { ...init, headers })
    if (!response.ok) {
      const error = (await response.json()) as ApiError
      throw new ApiClientError(error)
    }
    if (response.status === 204) return undefined as T
    return (await response.json()) as T
  }
}

export const api = new ApiClient()
