import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { server } from '@/test/server'
import { aUser, problem, renderWithRouter } from '@/test/utils'
import { CreateUserPage } from './CreateUserPage'

async function fillIn(email: string) {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Email'), email)
  await user.type(screen.getByLabelText('Full name'), 'Budi Santoso')
  await user.click(screen.getByRole('button', { name: 'Add and send invite' }))
}

describe('CreateUserPage', () => {
  it('creates the user as an employee without a phone number', async () => {
    const created = vi.fn()
    server.use(
      http.post('/api/admin/users', async ({ request }) => {
        created(await request.json())
        return HttpResponse.json(aUser({ status: 'PENDING' }), { status: 201 })
      }),
    )
    const onCreated = vi.fn()
    renderWithRouter(<CreateUserPage onCreated={onCreated} />)

    await fillIn('budi@example.com')

    await vi.waitFor(() => expect(onCreated).toHaveBeenCalled())
    expect(created).toHaveBeenCalledWith({
      email: 'budi@example.com',
      fullName: 'Budi Santoso',
      phone: null,
      role: 'EMPLOYEE',
    })
  })

  it('shows a taken email next to the field', async () => {
    server.use(
      http.post('/api/admin/users', () =>
        HttpResponse.json(problem(409, 'Email is already registered'), { status: 409 }),
      ),
    )
    renderWithRouter(<CreateUserPage onCreated={() => {}} />)

    await fillIn('budi@example.com')

    expect(await screen.findByText('Email is already registered')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
  })

  it('checks the form before sending it', async () => {
    renderWithRouter(<CreateUserPage onCreated={() => {}} />)

    await fillIn('not-an-email')

    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument()
  })
})
