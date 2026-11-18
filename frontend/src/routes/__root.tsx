import type { QueryClient } from '@tanstack/react-query'
import { createRootRouteWithContext, Link, Outlet } from '@tanstack/react-router'
import { AuthCard } from '@/components/AuthCard'
import { Button } from '@/components/ui/button'
import { Toaster } from '@/components/ui/sonner'

export interface RouterContext {
  queryClient: QueryClient
}

export const Route = createRootRouteWithContext<RouterContext>()({
  component: () => (
    <>
      <Outlet />
      <Toaster richColors position="top-center" />
    </>
  ),
  notFoundComponent: () => (
    <AuthCard title="Page not found" description="The page you are looking for does not exist.">
      <Button asChild className="w-full">
        <Link to="/">Go home</Link>
      </Button>
    </AuthCard>
  ),
})
