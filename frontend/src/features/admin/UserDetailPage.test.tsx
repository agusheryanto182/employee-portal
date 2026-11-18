import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { session } from '@/lib/session'
import { server } from '@/test/server'
import { aUser, anAuthResponse, renderWithRouter } from '@/test/utils'
import { UserDetailPage } from './UserDetailPage'

const ID = '00000000-0000-0000-0000-000000000001'
const emptyHistory = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }

function render(user = aUser()) {
  server.use(
    http.get(`/api/admin/users/${ID}`, () => HttpResponse.json(user)),
    http.get('/api/admin/login-history', () => HttpResponse.json(emptyHistory)),
  )
  return renderWithRouter(<UserDetailPage userId={ID} historyPage={0} onHistoryPageChange={() => {}} />)
}

describe('UserDetailPage', () => {
  it('sends only the fields that changed', async () => {
    const patched = vi.fn()
    server.use(
      http.patch(`/api/admin/users/${ID}`, async ({ request }) => {
        const body = await request.json()
        patched(body)
        return HttpResponse.json(aUser({ phone: '+62 811 1111', updatedAt: '2026-11-18T09:00:00Z' }))
      }),
    )
    render()
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Phone'), '+62 811 1111')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await vi.waitFor(() => expect(patched).toHaveBeenCalledWith({ phone: '+62 811 1111' }))
  })

  it('locks the role on the admin’s own account', async () => {
    const me = aUser({ role: 'ADMIN' })
    session.set(anAuthResponse('t', me))
    render(me)

    expect(await screen.findByLabelText('Role')).toBeDisabled()
    expect(screen.getByText('You cannot change your own role.')).toBeInTheDocument()

    // The locked role must not block saving the other fields.
    const patched = vi.fn()
    server.use(
      http.patch(`/api/admin/users/${ID}`, async ({ request }) => {
        patched(await request.json())
        return HttpResponse.json({ ...me, fullName: 'Budi S.', updatedAt: '2026-11-18T09:00:00Z' })
      }),
    )
    const user = userEvent.setup()
    await user.clear(screen.getByLabelText('Full name'))
    await user.type(screen.getByLabelText('Full name'), 'Budi S.')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))
    await vi.waitFor(() => expect(patched).toHaveBeenCalledWith({ fullName: 'Budi S.' }))
    await vi.waitFor(() => expect(session.get()?.user.fullName).toBe('Budi S.')) // top bar stays in sync
  })

  it('says when the user does not exist', async () => {
    server.use(
      http.get(`/api/admin/users/${ID}`, () => HttpResponse.json({ status: 404, detail: 'User not found' }, { status: 404 })),
    )
    renderWithRouter(<UserDetailPage userId={ID} historyPage={0} onHistoryPageChange={() => {}} />)

    expect(await screen.findByText('This user does not exist.')).toBeInTheDocument()
  })
})
