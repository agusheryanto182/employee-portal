import { createFileRoute } from '@tanstack/react-router'
import { z } from 'zod'
import { AuthCard } from '@/components/AuthCard'
import { SetPasswordForm } from '@/features/auth/SetPasswordForm'
import { SignInLinks } from '@/features/auth/SignInLinks'

export const Route = createFileRoute('/reset-password')({
  validateSearch: z.object({ token: z.string().optional() }),
  component: ResetPasswordPage,
})

function ResetPasswordPage() {
  const { token } = Route.useSearch()
  return (
    <AuthCard title="Reset your password">
      <SetPasswordForm type="RESET" token={token} signInLinks={<SignInLinks />} />
    </AuthCard>
  )
}
