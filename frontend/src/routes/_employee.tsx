import { createFileRoute, Outlet } from '@tanstack/react-router'
import { History, LayoutDashboard, UserRound } from 'lucide-react'
import { AppShell, type NavItem } from '@/components/AppShell'
import { useSession } from '@/lib/auth'
import { requireArea } from '@/lib/guards'

const NAV: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, exact: true },
  { to: '/history', label: 'History', icon: History },
  { to: '/profile', label: 'Profile', icon: UserRound },
]

/** Employee area (`/`). Admins are sent to `/admin` (ADR 0012). */
export const Route = createFileRoute('/_employee')({
  beforeLoad: ({ location }) => requireArea('employee', location.href),
  component: EmployeeLayout,
})

function EmployeeLayout() {
  const { user: initialUser } = Route.useRouteContext()
  const user = useSession()?.user ?? initialUser // live after a profile edit
  return (
    <AppShell user={user} portal="employee" nav={NAV} profilePath="/profile">
      <Outlet />
    </AppShell>
  )
}
