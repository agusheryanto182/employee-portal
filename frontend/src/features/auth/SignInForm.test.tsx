import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { session } from '@/lib/session'
import { server } from '@/test/server'
import { anAuthResponse, problem, renderWithQuery } from '@/test/utils'
import { SignInForm } from './SignInForm'

async function fillAndSubmit(email: string, password: string) {
  const user = userEvent.setup()
  if (email) await user.type(screen.getByLabelText('Email'), email)
  if (password) await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: 'Sign in' }))
}

describe('SignInForm', () => {
  it('validates before calling the API', async () => {
    renderWithQuery(<SignInForm portal="employee" onSignedIn={vi.fn()} />)

    await fillAndSubmit('not-an-email', '')

    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument()
    expect(screen.getByText('Enter your password')).toBeInTheDocument()
  })

  it('signs in on the endpoint of its own portal and keeps the session in memory', async () => {
    const onSignedIn = vi.fn()
    server.use(http.post('/api/admin/auth/login', () => HttpResponse.json(anAuthResponse('admin-token'))))
    renderWithQuery(<SignInForm portal="admin" onSignedIn={onSignedIn} />)

    await fillAndSubmit('admin@example.com', 'Secret123!')

    await vi.waitFor(() => expect(onSignedIn).toHaveBeenCalledOnce())
    expect(session.get()?.accessToken).toBe('admin-token')
  })

  it('shows one generic message for wrong credentials', async () => {
    server.use(
      http.post('/api/auth/login', () => HttpResponse.json(problem(401, 'Invalid email or password'), { status: 401 })),
    )
    renderWithQuery(<SignInForm portal="employee" onSignedIn={vi.fn()} />)

    await fillAndSubmit('budi@example.com', 'wrong')

    expect(await screen.findByText('Invalid email or password.')).toBeInTheDocument()
  })

  it('tells the user how long to wait after too many attempts', async () => {
    server.use(
      http.post('/api/auth/login', () =>
        HttpResponse.json(problem(429, 'Too many'), { status: 429, headers: { 'Retry-After': '600' } }),
      ),
    )
    renderWithQuery(<SignInForm portal="employee" onSignedIn={vi.fn()} />)

    await fillAndSubmit('budi@example.com', 'wrong')

    expect(await screen.findByText('Too many failed attempts. Try again in 10 minutes.')).toBeInTheDocument()
  })
})
