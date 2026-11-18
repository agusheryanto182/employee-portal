import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { session } from '@/lib/session'
import { server } from '@/test/server'
import { anAuthResponse, problem, renderWithQuery } from '@/test/utils'
import { ChangePasswordForm } from './ChangePasswordForm'

async function submit(current: string, next: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Current password'), current)
  await user.type(screen.getByLabelText('New password'), next)
  await user.type(screen.getByLabelText('Confirm new password'), next)
  await user.click(screen.getByRole('button', { name: 'Change password' }))
}

describe('ChangePasswordForm', () => {
  it('shows a wrong current password next to the field', async () => {
    session.set(anAuthResponse('t1'))
    server.use(
      http.put('/api/me/password', () =>
        HttpResponse.json(problem(400, 'Validation failed', { errors: { currentPassword: 'is incorrect' } }), {
          status: 400,
        }),
      ),
    )
    renderWithQuery(<ChangePasswordForm />)

    await submit('wrong-one', 'BrandNew123!')

    expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument()
    expect(session.get()?.accessToken).toBe('t1') // a 400 is not an expired session
  })

  it('keeps this device signed in with the new session', async () => {
    session.set(anAuthResponse('old'))
    server.use(http.put('/api/me/password', () => HttpResponse.json(anAuthResponse('after-change'))))
    renderWithQuery(<ChangePasswordForm />)

    await submit('Secret123!', 'BrandNew123!')

    await screen.findByLabelText('Current password')
    await vi.waitFor(() => expect(session.get()?.accessToken).toBe('after-change'))
  })
})
