import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import type { AuthResponse, User } from '@/lib/types'

/** Renders with a fresh query cache that never retries, so error states show immediately. */
export function renderWithQuery(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}

export function aUser(overrides: Partial<User> = {}): User {
  return {
    id: '00000000-0000-0000-0000-000000000001',
    email: 'budi@example.com',
    fullName: 'Budi Santoso',
    phone: null,
    role: 'EMPLOYEE',
    status: 'ACTIVE',
    createdAt: '2026-11-01T08:00:00Z',
    updatedAt: '2026-11-01T08:00:00Z',
    ...overrides,
  }
}

export function anAuthResponse(accessToken = 'access-1', user: User = aUser()): AuthResponse {
  return { accessToken, tokenType: 'Bearer', expiresIn: 900, user }
}

export function problem(status: number, detail: string, extra: Record<string, unknown> = {}) {
  return { type: 'about:blank', title: 'Error', status, detail, ...extra }
}
