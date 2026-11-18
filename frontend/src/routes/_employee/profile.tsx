import { createFileRoute } from '@tanstack/react-router'
import { ChangePasswordForm } from '@/features/profile/ChangePasswordForm'
import { ProfileForm } from '@/features/profile/ProfileForm'
import { useSession } from '@/lib/auth'

export const Route = createFileRoute('/_employee/profile')({
  component: ProfileRoute,
})

function ProfileRoute() {
  const { user: initialUser } = Route.useRouteContext()
  const user = useSession()?.user ?? initialUser
  return (
    <div className="grid gap-6 lg:grid-cols-2 lg:items-start">
      <ProfileForm user={user} />
      <ChangePasswordForm />
    </div>
  )
}
