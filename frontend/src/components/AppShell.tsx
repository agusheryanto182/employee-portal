import { Link, useNavigate, useRouter } from '@tanstack/react-router'
import { useQueryClient } from '@tanstack/react-query'
import type { LucideIcon } from 'lucide-react'
import { LogOut, UserRound } from 'lucide-react'
import type { ReactNode } from 'react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { signInPathFor, signOut, type Portal } from '@/lib/auth'
import { initials } from '@/lib/format'
import type { User } from '@/lib/types'
import { Logo } from './Logo'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  /** Only highlight on an exact match (for the area's home). */
  exact?: boolean
}

interface AppShellProps {
  user: User
  portal: Portal
  nav: NavItem[]
  profilePath: string
  subtitle?: string
  children: ReactNode
}

/**
 * Signed-in layout, mobile first: a top bar with the account menu, a bottom tab bar on phones and
 * inline navigation from the `sm` breakpoint up.
 */
export function AppShell({ user, portal, nav, profilePath, subtitle, children }: AppShellProps) {
  const navigate = useNavigate()
  const router = useRouter()
  const queryClient = useQueryClient()

  async function handleSignOut() {
    await signOut().catch(() => {})
    queryClient.clear() // never show the previous user's data to the next one
    await navigate({ to: signInPathFor(portal) })
    router.invalidate()
  }

  return (
    <div className="min-h-svh bg-muted/40 pb-20 sm:pb-0">
      <header className="sticky top-0 z-10 border-b bg-background/95 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-5xl items-center gap-6 px-4">
          <Link to={nav[0]!.to} aria-label="Home">
            <Logo subtitle={subtitle} />
          </Link>
          <nav aria-label="Main" className="hidden flex-1 items-center gap-1 sm:flex">
            {nav.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                activeOptions={{ exact: item.exact }}
                className="rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:text-foreground data-[status=active]:bg-muted data-[status=active]:font-medium data-[status=active]:text-foreground"
              >
                {item.label}
              </Link>
            ))}
          </nav>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon-lg" className="ml-auto rounded-full sm:ml-0" aria-label="Account menu">
                <Avatar>
                  <AvatarFallback>{initials(user.fullName)}</AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel className="grid">
                <span className="truncate">{user.fullName}</span>
                <span className="truncate text-xs font-normal text-muted-foreground">{user.email}</span>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild>
                <Link to={profilePath}>
                  <UserRound aria-hidden />
                  My profile
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={handleSignOut}>
                <LogOut aria-hidden />
                Sign out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-6">{children}</main>

      <nav aria-label="Main" className="fixed inset-x-0 bottom-0 z-10 border-t bg-background sm:hidden">
        <ul className="grid" style={{ gridTemplateColumns: `repeat(${nav.length}, minmax(0, 1fr))` }}>
          {nav.map((item) => (
            <li key={item.to}>
              <Link
                to={item.to}
                activeOptions={{ exact: item.exact }}
                className="flex flex-col items-center gap-1 py-2.5 text-xs text-muted-foreground data-[status=active]:font-medium data-[status=active]:text-foreground"
              >
                <item.icon className="size-5" aria-hidden />
                {item.label}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}
