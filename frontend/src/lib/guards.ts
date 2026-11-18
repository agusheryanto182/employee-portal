import { redirect } from '@tanstack/react-router'
import { homeFor, restoreSession, signInPathFor, type Portal } from './auth'
import type { User } from './types'

const ROLE_OF: Record<Portal, User['role']> = { employee: 'EMPLOYEE', admin: 'ADMIN' }

/**
 * beforeLoad guard for a signed-in area (ADR 0012): no session sends the user to that area's
 * sign-in page, and a user of the other role is sent to their own home. The API enforces the same
 * rules; this only keeps users out of screens they cannot use.
 */
export async function requireArea(portal: Portal, href: string): Promise<{ user: User }> {
  const current = await restoreSession()
  if (!current) {
    throw redirect({ to: signInPathFor(portal), search: { redirect: href } })
  }
  if (current.user.role !== ROLE_OF[portal]) {
    throw redirect({ to: homeFor(current.user.role) })
  }
  return { user: current.user }
}

/** beforeLoad guard for the sign-in pages: someone already signed in goes to their home. */
export async function redirectIfSignedIn(): Promise<void> {
  const current = await restoreSession()
  if (current) {
    throw redirect({ to: homeFor(current.user.role) })
  }
}
