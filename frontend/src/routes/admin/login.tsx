import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { z } from 'zod'
import { AuthCard } from '@/components/AuthCard'
import { SignInForm } from '@/features/auth/SignInForm'
import { afterSignInPath } from '@/lib/auth'
import { redirectIfSignedIn } from '@/lib/guards'

export const Route = createFileRoute('/admin/login')({
  validateSearch: z.object({ redirect: z.string().optional() }),
  beforeLoad: redirectIfSignedIn,
  component: AdminLoginPage,
})

function AdminLoginPage() {
  const { redirect } = Route.useSearch()
  const navigate = useNavigate()
  return (
    <AuthCard
      title="Backoffice sign in"
      description="For administrators only."
      footer={
        <span>
          Employee?{' '}
          <Link to="/login" className="font-medium text-foreground underline-offset-4 hover:underline">
            Sign in to your dashboard
          </Link>
        </span>
      }
    >
      <SignInForm portal="admin" onSignedIn={(s) => navigate({ href: afterSignInPath(s.user.role, redirect) })} />
    </AuthCard>
  )
}
