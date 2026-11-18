import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryHistory, createRootRoute, createRouter, RouterProvider } from '@tanstack/react-router'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import type { AuthResponse, User } from '@/lib/types'

function testQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
}

/** Renders with a fresh query cache that never retries, so error states show immediately. */
export function renderWithQuery(ui: ReactElement) {
  return render(<QueryClientProvider client={testQueryClient()}>{ui}</QueryClientProvider>)
}

/**
 * Like renderWithQuery, for components that render `<Link>`s: mounts the component as the only
 * route of an in-memory router. Wait for content with `findBy*`, because the router renders async.
 */
export function renderWithRouter(ui: ReactElement) {
  const router = createRouter({
    routeTree: createRootRoute({ component: () => ui }),
    history: createMemoryHistory({ initialEntries: ['/'] }),
  })
  const result = render(
    <QueryClientProvider client={testQueryClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { ...result, router }
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
