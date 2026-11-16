const TOKEN_KEY = 'settleup.access-token'

export function loadToken() {
  return window.localStorage.getItem(TOKEN_KEY)
}

export function saveToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY)
}
