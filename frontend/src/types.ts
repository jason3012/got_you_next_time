export type SplitStrategy = 'EQUAL' | 'EXACT' | 'PERCENTAGE'
export type MemberRole = 'ADMIN' | 'MEMBER'

export interface User {
  id: string
  email: string
  displayName: string
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  expiresInSeconds: number
  user: User
}

export interface ExpenseSuggestion {
  id: string
  transactionId: string
  groupId: string
  groupName: string
  merchantName: string
  amountCents: number
  isoCurrencyCode: string
  category: string | null
  postedDate: string
  confidence: number
  reasons: string[]
  status: 'PENDING' | 'CONFIRMED' | 'REJECTED'
  createdAt: string
}

export interface GroupMember {
  userId: string
  displayName: string
  email: string
  role: MemberRole
}

export interface Group {
  id: string
  name: string
  createdBy: string
  members: GroupMember[]
  createdAt: string
  updatedAt: string
}

export interface ExpenseSplit {
  userId: string
  shareCents: number
}

export interface Expense {
  id: string
  groupId: string
  payerId: string
  createdBy: string
  description: string
  amountCents: number
  splitStrategy: SplitStrategy
  splits: ExpenseSplit[]
  createdAt: string
}

export interface Balance {
  userId: string
  displayName: string
  balanceCents: number
}

export interface Transfer {
  fromUserId: string
  toUserId: string
  amountCents: number
}

export interface BankAccount {
  id: string
  name: string
  officialName: string | null
  mask: string | null
  type: string
  subtype: string | null
}

export interface BankConnection {
  id: string
  institutionName: string
  status: 'HEALTHY' | 'ERROR'
  errorCode: string | null
  lastSyncedAt: string | null
  createdAt: string
  accounts: BankAccount[]
}

export interface BankTransaction {
  id: string
  accountId: string
  accountName: string
  name: string
  merchantName: string | null
  amountCents: number
  isoCurrencyCode: string
  category: string | null
  authorizedDate: string | null
  postedDate: string
  pending: boolean
  expenseId: string | null
}

export interface SplitInput {
  userId: string
  amountCents?: number
  percentage?: number
}

export interface ApiError {
  timestamp: string
  status: number
  code: string
  message: string
  fieldErrors: Record<string, string>
}
