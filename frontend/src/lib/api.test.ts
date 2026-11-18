import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { anAuthResponse, problem } from '@/test/utils'
import { server } from '@/test/server'
import { api, setSessionExpiredHandler, toProblem } from './api'
import { session } from './session'

describe('api client', () => {
  it('sends the access token from memory', async () => {
    session.set(anAuthResponse('token-abc'))
    server.use(
      http.get('/api/me', ({ request }) => HttpResponse.json({ auth: request.headers.get('Authorization') })),
    )

    const response = await api.get('/me')

    expect(response.data.auth).toBe('Bearer token-abc')
  })

  it('refreshes once on 401 and retries with the new token', async () => {
    session.set(anAuthResponse('expired'))
    let refreshes = 0
    server.use(
      http.post('/api/auth/refresh', () => {
        refreshes++
        return HttpResponse.json(anAuthResponse('fresh'))
      }),
      http.get('/api/me', ({ request }) =>
        request.headers.get('Authorization') === 'Bearer fresh'
          ? HttpResponse.json({ ok: true })
          : HttpResponse.json(problem(401, 'Authentication required'), { status: 401 }),
      ),
    )

    // Two requests fail at the same time; only one refresh may happen (the token rotates).
    const [first, second] = await Promise.all([api.get('/me'), api.get('/me')])

    expect(first.data.ok).toBe(true)
    expect(second.data.ok).toBe(true)
    expect(refreshes).toBe(1)
    expect(session.get()?.accessToken).toBe('fresh')
  })

  it('clears the session and reports expiry when the refresh fails', async () => {
    session.set(anAuthResponse('expired'))
    const expired = vi.fn()
    setSessionExpiredHandler(expired)
    server.use(
      http.post('/api/auth/refresh', () => HttpResponse.json(problem(401, 'Invalid'), { status: 401 })),
      http.get('/api/me', () => HttpResponse.json(problem(401, 'Authentication required'), { status: 401 })),
    )

    await expect(api.get('/me')).rejects.toThrow()

    expect(session.get()).toBeNull()
    expect(expired).toHaveBeenCalledOnce()
  })

  it('does not try to refresh when signing in fails', async () => {
    let refreshes = 0
    server.use(
      http.post('/api/auth/refresh', () => {
        refreshes++
        return HttpResponse.json(anAuthResponse())
      }),
      http.post('/api/auth/login', () => HttpResponse.json(problem(401, 'Invalid email or password'), { status: 401 })),
    )

    await expect(api.post('/auth/login', {})).rejects.toThrow()

    expect(refreshes).toBe(0)
  })

  it('turns errors into problems with field errors and Retry-After', async () => {
    server.use(
      http.post('/api/x', () =>
        HttpResponse.json(problem(429, 'Too many'), { status: 429, headers: { 'Retry-After': '120' } }),
      ),
      http.post('/api/y', () =>
        HttpResponse.json(problem(400, 'Validation failed', { errors: { email: 'bad' } }), { status: 400 }),
      ),
    )

    expect(toProblem(await api.post('/x').catch((e: unknown) => e))).toMatchObject({ status: 429, retryAfterSeconds: 120 })
    expect(toProblem(await api.post('/y').catch((e: unknown) => e))).toMatchObject({ status: 400, errors: { email: 'bad' } })
    expect(toProblem(new Error('offline')).status).toBe(0)
  })
})
