import { isRedirect } from '@tanstack/react-router'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { server } from '@/test/server'
import { aUser, anAuthResponse, problem } from '@/test/utils'
import { redirectIfSignedIn, requireArea } from './guards'
import { OfflineError } from './online'
import { session } from './session'

async function redirectOf(promise: Promise<unknown>) {
  const thrown = await promise.then(
    () => null,
    (error: unknown) => error,
  )
  expect(isRedirect(thrown)).toBe(true)
  return (thrown as { options: { to: string; search?: Record<string, string> } }).options
}

describe('route guards', () => {
  it('sends a visitor without a session to the sign-in page of the area, remembering the page', async () => {
    server.use(http.post('/api/auth/refresh', () => HttpResponse.json(problem(401, 'Invalid'), { status: 401 })))

    const target = await redirectOf(requireArea('admin', '/admin/account'))

    expect(target.to).toBe('/admin/login')
    expect(target.search).toEqual({ redirect: '/admin/account' })
  })

  it('keeps each role in its own area', async () => {
    session.set(anAuthResponse('t', aUser({ role: 'ADMIN' })))
    expect((await redirectOf(requireArea('employee', '/history'))).to).toBe('/admin')

    session.set(anAuthResponse('t', aUser({ role: 'EMPLOYEE' })))
    expect((await redirectOf(requireArea('admin', '/admin'))).to).toBe('/')
    await expect(requireArea('employee', '/')).resolves.toMatchObject({ user: { role: 'EMPLOYEE' } })
  })

  it('sends a signed-in user away from the sign-in pages', async () => {
    session.set(anAuthResponse('t', aUser({ role: 'EMPLOYEE' })))
    expect((await redirectOf(redirectIfSignedIn())).to).toBe('/')
  })

  it('shows the offline screen instead of the sign-in page when the session cannot be checked', async () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false)
    server.use(http.post('/api/auth/refresh', () => HttpResponse.error()))

    await expect(requireArea('employee', '/history')).rejects.toBeInstanceOf(OfflineError)
  })
})
