import { useSyncExternalStore } from 'react'
import { api, refreshSession } from './api'
import { isOffline } from './online'
import { session, type Session } from './session'
import type { AuthResponse, Role } from './types'

export type Portal = 'employee' | 'admin'

const LOGIN_PATH: Record<Portal, string> = {
  employee: '/auth/login',
  admin: '/admin/auth/login',
}

/** Each area has its own sign-in endpoint, and the API only accepts its own role (ADR 0012). */
export async function signIn(portal: Portal, email: string, password: string): Promise<Session> {
  const response = await api.post<AuthResponse>(LOGIN_PATH[portal], { email, password })
  session.set(response.data)
  return session.get()!
}

export async function signOut(): Promise<void> {
  try {
    await api.post('/auth/logout')
  } finally {
    session.clear()
    forgetRestoreAttempt()
  }
}

let restoring: Promise<Session | null> | null = null

/** Lets the next restoreSession() try the refresh cookie again (after sign-out, and between tests). */
export function forgetRestoreAttempt() {
  restoring = null
}

/**
 * On the first protected navigation after a page load, tries the refresh cookie to get a session
 * back. Concurrent callers share the attempt, and a failed attempt is remembered so later
 * navigations do not ask again. A successful one is not: the session store is the source of truth,
 * so a session that later expires cannot come back from this cache.
 */
export function restoreSession(): Promise<Session | null> {
  const current = session.get()
  if (current) {
    return Promise.resolve(current)
  }
  restoring ??= refreshSession().then(() => {
    const restored = session.get()
    // Offline: the cookie was never checked, so try again once the device is back online.
    if (restored || isOffline()) {
      restoring = null
    }
    return restored
  })
  return restoring
}

export function useSession(): Session | null {
  return useSyncExternalStore(session.subscribe, session.get, session.get)
}

export function homeFor(role: Role): '/' | '/admin' {
  return role === 'ADMIN' ? '/admin' : '/'
}

export function signInPathFor(portal: Portal): '/login' | '/admin/login' {
  return portal === 'admin' ? '/admin/login' : '/login'
}

/**
 * Where to go after signing in: the page the user was sent away from, if it is a local path in
 * their own area, otherwise their home. Rejects "//evil.com" and other open redirects.
 */
export function afterSignInPath(role: Role, redirect: string | undefined): string {
  const home = homeFor(role)
  if (!redirect || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return home
  }
  const inAdminArea = redirect === '/admin' || redirect.startsWith('/admin/')
  if (inAdminArea !== (role === 'ADMIN') || redirect.startsWith('/admin/login') || redirect.startsWith('/login')) {
    return home
  }
  return redirect
}
