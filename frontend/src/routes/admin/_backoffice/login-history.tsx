import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { z } from 'zod'
import { LoginHistoryPage } from '@/features/admin/LoginHistoryPage'

const day = z.string().regex(/^\d{4}-\d{2}-\d{2}$/)

export const Route = createFileRoute('/admin/_backoffice/login-history')({
  validateSearch: z.object({
    userId: z.uuid().optional().catch(undefined),
    outcome: z.enum(['success', 'failed']).optional().catch(undefined),
    from: day.optional().catch(undefined),
    to: day.optional().catch(undefined),
    page: z.coerce.number().int().min(0).optional().catch(undefined),
  }),
  component: LoginHistoryRoute,
})

function LoginHistoryRoute() {
  const filters = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  return <LoginHistoryPage filters={filters} onFiltersChange={(search) => navigate({ search, replace: true })} />
}
