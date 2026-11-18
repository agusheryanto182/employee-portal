import type { AuthResponse, User } from './types'

export interface Session {
  accessToken: string
  user: User
}

type Listener = () => void

/**
 * The signed-in session, kept only in memory (docs/decisions/0003-jwt-with-refresh-cookie.md).
 * Nothing goes to localStorage: a page reload restores the session from the httpOnly refresh
 * cookie instead (see restoreSession in auth.ts).
 */
let current: Session | null = null
const listeners = new Set<Listener>()

function emit() {
  listeners.forEach((listener) => listener())
}

export const session = {
  get: (): Session | null => current,

  set(response: AuthResponse) {
    current = { accessToken: response.accessToken, user: response.user }
    emit()
  },

  updateUser(user: User) {
    if (current) {
      current = { ...current, user }
      emit()
    }
  },

  clear() {
    if (current) {
      current = null
      emit()
    }
  },

  subscribe(listener: Listener) {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },
}
