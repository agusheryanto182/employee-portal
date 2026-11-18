import { createFileRoute } from '@tanstack/react-router'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { useSession } from '@/lib/auth'

export const Route = createFileRoute('/_employee/')({
  component: DashboardRoute,
})

function DashboardRoute() {
  const { user: initialUser } = Route.useRouteContext()
  return <DashboardPage user={useSession()?.user ?? initialUser} />
}
