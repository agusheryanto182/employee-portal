import { createFileRoute } from '@tanstack/react-router'
import { z } from 'zod'
import { AuthCard } from '@/components/AuthCard'
import { SetPasswordForm } from '@/features/auth/SetPasswordForm'
import { SignInLinks } from '@/features/auth/SignInLinks'

export const Route = createFileRoute('/activate')({
  validateSearch: z.object({ token: z.string().optional() }),
  component: ActivatePage,
})

function ActivatePage() {
  const { token } = Route.useSearch()
  return (
    <AuthCard title="Activate your account">
      <SetPasswordForm type="INVITE" token={token} signInLinks={<SignInLinks />} />
    </AuthCard>
  )
}
