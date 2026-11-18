import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { z } from 'zod'
import { UsersPage } from '@/features/admin/UsersPage'

export const Route = createFileRoute('/admin/_backoffice/users/')({
  validateSearch: z.object({
    q: z.string().optional().catch(undefined),
    status: z.enum(['PENDING', 'ACTIVE', 'INACTIVE']).optional().catch(undefined),
    role: z.enum(['ADMIN', 'EMPLOYEE']).optional().catch(undefined),
    page: z.coerce.number().int().min(0).optional().catch(undefined),
  }),
  component: UsersRoute,
})

function UsersRoute() {
  const filters = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  return <UsersPage filters={filters} onFiltersChange={(search) => navigate({ search, replace: true })} />
}
