import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { server } from '@/test/server'
import { aUser, problem, renderWithQuery } from '@/test/utils'
import { UserActions } from './UserActions'

const ID = '00000000-0000-0000-0000-000000000001'

describe('UserActions', () => {
  it('resends an invite and shows the link to copy', async () => {
    server.use(
      http.post(`/api/admin/users/${ID}/resend-invite`, () =>
        HttpResponse.json({ link: 'http://localhost:5173/activate?token=abc' }),
      ),
    )
    renderWithQuery(<UserActions user={aUser({ status: 'PENDING' })} isSelf={false} />)

    expect(screen.queryByRole('button', { name: 'Send password reset' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Resend invite' }))

    expect(await screen.findByLabelText('Link')).toHaveValue('http://localhost:5173/activate?token=abc')
    expect(screen.getByText('Invite link')).toBeInTheDocument()
  })

  it('asks before deactivating', async () => {
    const deactivated = vi.fn()
    server.use(
      http.post(`/api/admin/users/${ID}/deactivate`, () => {
        deactivated()
        return HttpResponse.json(aUser({ status: 'INACTIVE' }))
      }),
    )
    renderWithQuery(<UserActions user={aUser({ status: 'ACTIVE' })} isSelf={false} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Deactivate' }))
    const dialog = await screen.findByRole('alertdialog')
    expect(dialog).toHaveTextContent('Deactivate Budi Santoso?')
    expect(deactivated).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Deactivate' }))
    await vi.waitFor(() => expect(deactivated).toHaveBeenCalledOnce())
  })

  it('offers reactivation for a deactivated user', async () => {
    server.use(
      http.post(`/api/admin/users/${ID}/activate`, () => HttpResponse.json(problem(409, 'Already active'), { status: 409 })),
    )
    renderWithQuery(<UserActions user={aUser({ status: 'INACTIVE' })} isSelf={false} />)

    expect(screen.queryByRole('button', { name: 'Deactivate' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reactivate' })).toBeEnabled()
  })

  it('never lets admins deactivate themselves', () => {
    renderWithQuery(<UserActions user={aUser({ role: 'ADMIN', status: 'ACTIVE' })} isSelf />)

    expect(screen.queryByRole('button', { name: 'Deactivate' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Send password reset' })).toBeInTheDocument()
  })
})
