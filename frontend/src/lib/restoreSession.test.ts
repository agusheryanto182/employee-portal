import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { server } from '@/test/server'
import { anAuthResponse, problem } from '@/test/utils'
import { restoreSession } from './auth'
import { session } from './session'

function countRefreshes(respond: () => Response) {
  const calls = vi.fn()
  server.use(
    http.post('/api/auth/refresh', () => {
      calls()
      return respond()
    }),
  )
  return calls
}

describe('restoreSession', () => {
  it('asks the API once when there is no refresh cookie', async () => {
    const calls = countRefreshes(() => HttpResponse.json(problem(401, 'Invalid'), { status: 401 }))

    await expect(Promise.all([restoreSession(), restoreSession()])).resolves.toEqual([null, null])
    await expect(restoreSession()).resolves.toBeNull()

    expect(calls).toHaveBeenCalledOnce()
  })

  it('does not bring back a session that expired after it was restored', async () => {
    const calls = countRefreshes(() => HttpResponse.json(anAuthResponse('restored')))
    expect((await restoreSession())?.accessToken).toBe('restored')

    session.clear() // later the refresh token expires and the API client gives up
    server.use(http.post('/api/auth/refresh', () => HttpResponse.json(problem(401, 'Expired'), { status: 401 })))

    await expect(restoreSession()).resolves.toBeNull()
    expect(calls).toHaveBeenCalledOnce()
  })

  it('tries again after coming back online', async () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false)
    countRefreshes(() => HttpResponse.error())
    await expect(restoreSession()).resolves.toBeNull()

    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true)
    server.use(http.post('/api/auth/refresh', () => HttpResponse.json(anAuthResponse('back-online'))))
    expect((await restoreSession())?.accessToken).toBe('back-online')
  })
})
