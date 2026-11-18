import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import type { User } from '@/lib/types'
import { server } from '@/test/server'
import { aUser, renderWithRouter } from '@/test/utils'
import { UsersPage } from './UsersPage'

function pageOf(users: User[]) {
  return { content: users, page: 0, size: 20, totalElements: users.length, totalPages: 1 }
}

describe('UsersPage', () => {
  it('lists users with their role and status, linking to each one', async () => {
    server.use(
      http.get('/api/admin/users', () =>
        HttpResponse.json(
          pageOf([
            aUser({ id: 'a1', fullName: 'Budi Santoso', status: 'PENDING' }),
            aUser({ id: 'a2', fullName: 'Siti Rahma', email: 'siti@example.com', role: 'ADMIN' }),
          ]),
        ),
      ),
    )
    renderWithRouter(<UsersPage filters={{}} onFiltersChange={() => {}} />)

    const budi = (await screen.findByRole('link', { name: 'Budi Santoso' })).closest('tr')!
    expect(within(budi).getByText('Invited')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Siti Rahma' })).toHaveAttribute('href', '/admin/users/a2')
    expect(screen.getByText('2 users')).toBeInTheDocument()
  })

  it('sends the filters from the URL to the API', async () => {
    const requested = vi.fn()
    server.use(
      http.get('/api/admin/users', ({ request }) => {
        requested(Object.fromEntries(new URL(request.url).searchParams))
        return HttpResponse.json(pageOf([]))
      }),
    )
    renderWithRouter(<UsersPage filters={{ q: 'budi', status: 'INACTIVE', page: 2 }} onFiltersChange={() => {}} />)

    expect(await screen.findByText('No users match these filters.')).toBeInTheDocument()
    expect(requested).toHaveBeenCalledWith({ search: 'budi', status: 'INACTIVE', page: '2', size: '20' })
  })

  it('goes back to the first page when a filter changes', async () => {
    server.use(http.get('/api/admin/users', () => HttpResponse.json(pageOf([aUser()]))))
    const onFiltersChange = vi.fn()
    renderWithRouter(<UsersPage filters={{ page: 3 }} onFiltersChange={onFiltersChange} />)
    const user = userEvent.setup()

    await user.selectOptions(await screen.findByLabelText('Status'), 'PENDING')
    expect(onFiltersChange).toHaveBeenLastCalledWith({ status: 'PENDING', page: undefined })

    await user.type(screen.getByLabelText('Search by name or email'), ' siti ')
    await vi.waitFor(() => expect(onFiltersChange).toHaveBeenLastCalledWith({ q: 'siti', page: undefined }))
  })
})
