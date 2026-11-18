import { createFileRoute, Outlet } from '@tanstack/react-router'
import { History, UserRound, Users } from 'lucide-react'
import { AppShell, type NavItem } from '@/components/AppShell'
import { useSession } from '@/lib/auth'
import { requireArea } from '@/lib/guards'

const NAV: NavItem[] = [
  { to: '/admin/users', label: 'Users', icon: Users },
  { to: '/admin/login-history', label: 'Login history', icon: History },
  { to: '/admin/account', label: 'My account', icon: UserRound },
]

/** Backoffice (`/admin`). Employees are sent to `/` (ADR 0012). */
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
