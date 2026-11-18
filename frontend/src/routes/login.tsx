import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { z } from 'zod'
import { AuthCard } from '@/components/AuthCard'
import { SignInForm } from '@/features/auth/SignInForm'
import { afterSignInPath } from '@/lib/auth'
import { redirectIfSignedIn } from '@/lib/guards'

export const Route = createFileRoute('/login')({
  validateSearch: z.object({ redirect: z.string().optional() }),
  beforeLoad: redirectIfSignedIn,
  component: EmployeeLoginPage,
})

function EmployeeLoginPage() {
  const { redirect } = Route.useSearch()
  const navigate = useNavigate()
  return (
    <AuthCard
      title="Sign in"
      description="Sign in to your employee dashboard."
      footer={
        <span>
          Administrator?{' '}
          <Link to="/admin/login" className="font-medium text-foreground underline-offset-4 hover:underline">
            Sign in to the backoffice
          </Link>
        </span>
      }
    >
      <SignInForm portal="employee" onSignedIn={(s) => navigate({ href: afterSignInPath(s.user.role, redirect) })} />
    </AuthCard>
  )
}
