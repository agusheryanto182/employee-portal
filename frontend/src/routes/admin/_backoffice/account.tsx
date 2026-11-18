import { createFileRoute } from '@tanstack/react-router'
import { ChangePasswordForm } from '@/features/profile/ChangePasswordForm'
import { ProfileForm } from '@/features/profile/ProfileForm'
import { useSession } from '@/lib/auth'

/** "My account" for admins: the same self-service API as the employee profile (ADR 0012). */
export const Route = createFileRoute('/admin/_backoffice/account')({
  component: AccountRoute,
})

function AccountRoute() {
  const { user: initialUser } = Route.useRouteContext()
  const user = useSession()?.user ?? initialUser
  return (
    <div className="grid gap-6 lg:grid-cols-2 lg:items-start">
      <ProfileForm user={user} />
      <ChangePasswordForm />
    </div>
  )
}
