import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '@/test/server'
import { problem, renderWithQuery } from '@/test/utils'
import { SetPasswordForm } from './SetPasswordForm'

const links = <p>sign-in links</p>

describe('SetPasswordForm', () => {
  it('reports an invalid or expired link without showing the form', async () => {
    server.use(
      http.get('/api/auth/tokens/:token', () =>
        HttpResponse.json(problem(400, 'This link is invalid or has expired'), { status: 400 }),
      ),
    )
    renderWithQuery(<SetPasswordForm type="INVITE" token="old" signInLinks={links} />)

    expect(await screen.findByText('This link is invalid or has expired')).toBeInTheDocument()
    expect(screen.queryByLabelText('New password')).not.toBeInTheDocument()
  })

  it('treats a reset link opened on the activate page as invalid', async () => {
    server.use(
      http.get('/api/auth/tokens/:token', () =>
        HttpResponse.json({ type: 'RESET', email: 'budi@example.com', fullName: 'Budi' }),
      ),
    )
    renderWithQuery(<SetPasswordForm type="INVITE" token="reset-token" signInLinks={links} />)

    expect(await screen.findByText('This link is invalid or has expired')).toBeInTheDocument()
  })

  it('activates the account and then offers the sign-in links', async () => {
    let body: unknown
    server.use(
      http.get('/api/auth/tokens/:token', () =>
        HttpResponse.json({ type: 'INVITE', email: 'budi@example.com', fullName: 'Budi Santoso' }),
      ),
      http.post('/api/auth/activate', async ({ request }) => {
        body = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderWithQuery(<SetPasswordForm type="INVITE" token="invite-token" signInLinks={links} />)
    const user = userEvent.setup()

    expect(await screen.findByText('budi@example.com')).toBeInTheDocument()
    await user.type(screen.getByLabelText('New password'), 'BrandNew123!')
    await user.type(screen.getByLabelText('Confirm password'), 'Different123!')
    await user.click(screen.getByRole('button', { name: 'Activate account' }))
    expect(await screen.findByText('Passwords do not match')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Confirm password'))
    await user.type(screen.getByLabelText('Confirm password'), 'BrandNew123!')
    await user.click(screen.getByRole('button', { name: 'Activate account' }))

    expect(await screen.findByText('sign-in links')).toBeInTheDocument()
    expect(body).toEqual({ token: 'invite-token', password: 'BrandNew123!' })
  })
})
