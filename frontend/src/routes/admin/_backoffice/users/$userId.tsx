import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { z } from 'zod'
import { UserDetailPage } from '@/features/admin/UserDetailPage'

export const Route = createFileRoute('/admin/_backoffice/users/$userId')({
  validateSearch: z.object({ page: z.coerce.number().int().min(0).optional().catch(undefined) }),
  component: UserRoute,
})

function UserRoute() {
  const { userId } = Route.useParams()
  const { page = 0 } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  return (
    <UserDetailPage
      userId={userId}
      historyPage={page}
      onHistoryPageChange={(next) => navigate({ search: { page: next }, replace: true })}
    />
  )
}
