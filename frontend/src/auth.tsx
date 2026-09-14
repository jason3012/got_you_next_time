import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from './api'
import { clearToken, loadToken, saveToken } from './auth-storage'
import type { User } from './types'

type AuthContextValue = {
  loading: boolean
  user: User | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, displayName: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = loadToken()
    if (!token) { setLoading(false); return }
    api.setToken(token)
    api.me().then(setUser).catch(() => { clearToken(); api.setToken(null) }).finally(() => setLoading(false))
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    loading,
    user,
    async login(email, password) {
      const response = await api.login(email, password)
      saveToken(response.accessToken); api.setToken(response.accessToken); setUser(response.user)
    },
    async register(email, displayName, password) {
      const response = await api.register(email, displayName, password)
      saveToken(response.accessToken); api.setToken(response.accessToken); setUser(response.user)
    },
    logout() { clearToken(); api.setToken(null); setUser(null) },
  }), [loading, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
