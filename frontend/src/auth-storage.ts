const TOKEN_KEY = 'settleup.access-token'
const REFRESH_TOKEN_KEY = 'settleup.refresh-token'

export function loadToken() {
  return window.localStorage.getItem(TOKEN_KEY)
}

export function loadRefreshToken() {
  return window.localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function saveTokens(token: string, refreshToken: string) {
  window.localStorage.setItem(TOKEN_KEY, token)
  window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(REFRESH_TOKEN_KEY)
}
