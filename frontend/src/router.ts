import { QueryClient } from '@tanstack/react-query'
import { createRouter } from '@tanstack/react-router'
import { RouteError } from './components/RouteError'
import { setSessionExpiredHandler } from './lib/api'
import { routeTree } from './routeTree.gen'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // 4xx answers (bad link, forbidden) will not change on retry.
      retry: (failureCount, error) => {
        const status = (error as { response?: { status?: number } }).response?.status
        return status !== undefined && status < 500 ? false : failureCount < 2
      },
    },
  },
})

export const router = createRouter({
  routeTree,
  context: { queryClient },
  defaultPreload: 'intent',
  defaultErrorComponent: RouteError,
  scrollRestoration: true,
})

// A request got 401 and the refresh cookie could not fix it: back to this area's sign-in page.
setSessionExpiredHandler(() => {
  const { pathname, href } = router.state.location
  if (pathname === '/login' || pathname === '/admin/login') return
  queryClient.clear()
  const to = pathname === '/admin' || pathname.startsWith('/admin/') ? '/admin/login' : '/login'
  void router.navigate({ to, search: { redirect: href } })
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
