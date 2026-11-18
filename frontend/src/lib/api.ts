import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { session } from './session'
import type { AuthResponse } from './types'

/** Same-origin API: Vite proxies /api in development, Vercel rewrites it in production. */
export const api = axios.create({ baseURL: '/api', withCredentials: true })

/** A bare client for the refresh call itself, so it never goes through the retry interceptor. */
const refreshClient = axios.create({ baseURL: '/api', withCredentials: true })

let refreshing: Promise<boolean> | null = null
let onSessionExpired: () => void = () => {}

/** Called when a request gets 401 and the refresh cookie cannot fix it (the router sends the user to sign in). */
export function setSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler
}

/**
 * Exchanges the refresh cookie for a new access token. Concurrent callers share one request, so
 * several 401s at once cause a single refresh (a second one would present an already rotated token).
 */
export function refreshSession(): Promise<boolean> {
  refreshing ??= refreshClient
    .post<AuthResponse>('/auth/refresh')
    .then((response) => {
      session.set(response.data)
      return true
    })
    .catch(() => {
      session.clear()
      return false
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

api.interceptors.request.use((config) => {
  const token = session.get()?.accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

type RetryableConfig = AxiosRequestConfig & { _retried?: boolean }

/** Sign-in and refresh answer 401 for wrong credentials, which a refresh cannot fix. */
const NO_REFRESH = ['/auth/login', '/admin/auth/login', '/auth/refresh', '/auth/logout']

api.interceptors.response.use(undefined, async (error: AxiosError) => {
  const config = error.config as RetryableConfig | undefined
  const shouldRefresh =
    error.response?.status === 401 &&
    config !== undefined &&
    !config._retried &&
    !NO_REFRESH.includes(config.url ?? '')
  if (!shouldRefresh) {
    throw error
  }

  config._retried = true // refresh at most once per request
  if (await refreshSession()) {
    return api(config)
  }
  onSessionExpired()
  throw error
})

/** The RFC 9457 body the backend sends for every error. */
export interface Problem {
  status: number
  detail: string
  errors?: Record<string, string>
  retryAfterSeconds?: number
}

export function toProblem(error: unknown): Problem {
  if (axios.isAxiosError(error) && error.response) {
    const body = (error.response.data ?? {}) as Partial<Problem>
    const retryAfter = Number(error.response.headers['retry-after'])
    return {
      status: error.response.status,
      detail: body.detail ?? 'Something went wrong. Please try again.',
      errors: body.errors,
      retryAfterSeconds: Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : undefined,
    }
  }
  return { status: 0, detail: 'Cannot reach the server. Check your connection and try again.' }
}
