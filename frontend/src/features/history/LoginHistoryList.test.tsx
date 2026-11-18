import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { LoginHistoryEntry } from '@/lib/types'
import { LoginHistoryList } from './LoginHistoryList'

const base: LoginHistoryEntry = {
  id: '1',
  userId: 'u1',
  email: 'budi@example.com',
  success: true,
  failureReason: null,
  ipAddress: '203.0.113.7',
  userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36',
  createdAt: '2026-11-18T08:00:00Z',
}

describe('LoginHistoryList', () => {
  it('shows successful and failed attempts with the reason, device and IP', () => {
    render(
      <LoginHistoryList
        entries={[base, { ...base, id: '2', success: false, failureReason: 'INVALID_CREDENTIALS' }]}
      />,
    )

    expect(screen.getByText('Signed in')).toBeInTheDocument()
    expect(screen.getByText('Wrong email or password')).toBeInTheDocument()
    expect(screen.getByText('Failed')).toBeInTheDocument()
    expect(screen.getAllByText('Chrome on Linux')).toHaveLength(2)
    expect(screen.getAllByText('IP 203.0.113.7')).toHaveLength(2)
  })

  it('has an empty state', () => {
    render(<LoginHistoryList entries={[]} />)
    expect(screen.getByText('No sign-in attempts yet.')).toBeInTheDocument()
  })
})
