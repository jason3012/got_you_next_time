import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from './api'
import { clearToken, loadRefreshToken, loadToken, saveTokens } from './auth-storage'
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
    api.onUnauthorized(() => { clearToken(); setUser(null) })
    const token = loadToken()
    if (!token) {
      setLoading(false)
      return () => api.onUnauthorized(() => {})
    }
    api.setTokens(token, loadRefreshToken())
    api.me().then(setUser).catch(() => { clearToken(); api.setTokens(null) }).finally(() => setLoading(false))
    return () => api.onUnauthorized(() => {})
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    loading,
    user,
    async login(email, password) {
      const response = await api.login(email, password)
      saveTokens(response.accessToken, response.refreshToken); api.setTokens(response.accessToken, response.refreshToken); setUser(response.user)
    },
    async register(email, displayName, password) {
      const response = await api.register(email, displayName, password)
      saveTokens(response.accessToken, response.refreshToken); api.setTokens(response.accessToken, response.refreshToken); setUser(response.user)
    },
    logout() { clearToken(); api.setTokens(null); setUser(null) },
  }), [loading, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
