import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import type { LoginHistoryEntry } from '@/lib/types'
import { server } from '@/test/server'
import { renderWithRouter } from '@/test/utils'
import { LoginHistoryPage } from './LoginHistoryPage'

const unknownEmail: LoginHistoryEntry = {
  id: 'h1',
  userId: null,
  email: 'nobody@example.com',
  success: false,
  failureReason: 'INVALID_CREDENTIALS',
  ipAddress: '10.0.0.1',
  userAgent: null,
  createdAt: '2026-11-18T08:00:00Z',
}

describe('LoginHistoryPage', () => {
  it('sends the result and date filters and marks unregistered emails', async () => {
    const requested = vi.fn()
    server.use(
      http.get('/api/admin/login-history', ({ request }) => {
        requested(Object.fromEntries(new URL(request.url).searchParams))
        return HttpResponse.json({ content: [unknownEmail], page: 0, size: 20, totalElements: 1, totalPages: 1 })
      }),
    )
    renderWithRouter(
      <LoginHistoryPage filters={{ outcome: 'failed', from: '2026-11-18', to: '2026-11-18' }} onFiltersChange={() => {}} />,
    )

    expect(await screen.findByText('(not registered)')).toBeInTheDocument()
    expect(screen.getByText('Wrong email or password')).toBeInTheDocument()
    expect(requested).toHaveBeenCalledWith({
      success: 'false',
      from: new Date(2026, 10, 18).toISOString(),
      to: new Date(2026, 10, 19).toISOString(),
      page: '0',
      size: '20',
    })
  })
})
