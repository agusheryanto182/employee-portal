import { createFileRoute, Outlet } from '@tanstack/react-router'
import { LayoutDashboard, UserRound } from 'lucide-react'
import { AppShell, type NavItem } from '@/components/AppShell'
import { useSession } from '@/lib/auth'
import { requireArea } from '@/lib/guards'

const NAV: NavItem[] = [
  { to: '/admin', label: 'Overview', icon: LayoutDashboard, exact: true },
  { to: '/admin/account', label: 'My account', icon: UserRound },
]

/** Backoffice (`/admin`). Employees are sent to `/` (ADR 0012). Screens are filled in by issue #11. */
export const Route = createFileRoute('/admin/_backoffice')({
  beforeLoad: ({ location }) => requireArea('admin', location.href),
  component: BackofficeLayout,
})

function BackofficeLayout() {
  const { user: initialUser } = Route.useRouteContext()
  const user = useSession()?.user ?? initialUser
  return (
    <AppShell user={user} portal="admin" nav={NAV} profilePath="/admin/account" subtitle="Backoffice">
      <Outlet />
    </AppShell>
  )
}
