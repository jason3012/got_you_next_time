import type {
  ApiError,
  AuthResponse,
  Balance,
  BankConnection,
  BankTransaction,
  Expense,
  ExpenseSuggestion,
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
  private refreshToken: string | null = null
  private refreshPromise: Promise<AuthResponse> | null = null
  private onSessionExpired: (() => void) | null = null

  setTokens(token: string | null, refreshToken: string | null = null) {
    this.token = token
    this.refreshToken = refreshToken
  }

  onUnauthorized(handler: () => void) {
    this.onSessionExpired = handler
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

  suggestions() {
    return this.request<ExpenseSuggestion[]>('/suggestions')
  }

  confirmSuggestion(suggestionId: string) {
    return this.request(`/suggestions/${suggestionId}/confirm`, {
      method: 'POST',
      body: JSON.stringify({ splitStrategy: 'EQUAL', splits: [] }),
    })
  }

  rejectSuggestion(suggestionId: string) {
    return this.request<ExpenseSuggestion>(`/suggestions/${suggestionId}/reject`, { method: 'POST' })
  }

  private async request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
    const headers = new Headers(init.headers)
    if (init.body) headers.set('Content-Type', 'application/json')
    if (this.token) headers.set('Authorization', `Bearer ${this.token}`)
    const response = await fetch(`${API_URL}${path}`, { ...init, headers })
    if (response.status === 401 && retry && this.refreshToken && path !== '/auth/refresh') {
      try {
        const refreshed = await this.refreshAccessToken()
        this.setTokens(refreshed.accessToken, refreshed.refreshToken)
        window.localStorage.setItem('settleup.access-token', refreshed.accessToken)
        window.localStorage.setItem('settleup.refresh-token', refreshed.refreshToken)
        return this.request<T>(path, init, false)
      } catch {
        this.setTokens(null, null)
        this.onSessionExpired?.()
      }
    }
    if (!response.ok) {
      const error = await response.json().catch(() => ({
        timestamp: new Date().toISOString(), status: response.status, code: 'REQUEST_FAILED',
        message: 'The request could not be completed.', fieldErrors: {},
      })) as ApiError
      throw new ApiClientError(error)
    }
    if (response.status === 204) return undefined as T
    return (await response.json()) as T
  }

  private refreshAccessToken() {
    if (!this.refreshPromise) {
      this.refreshPromise = fetch(`${API_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: this.refreshToken }),
      }).then(async (response) => {
        if (!response.ok) throw new Error('Session refresh failed')
        return response.json() as Promise<AuthResponse>
      }).finally(() => { this.refreshPromise = null })
    }
    return this.refreshPromise
  }
}

export const api = new ApiClient()
