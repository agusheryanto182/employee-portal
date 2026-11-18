import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { z } from 'zod'
import { HistoryPage } from '@/features/history/HistoryPage'

export const Route = createFileRoute('/_employee/history')({
  validateSearch: z.object({ page: z.coerce.number().int().min(0).catch(0).optional() }),
  component: HistoryRoute,
})

function HistoryRoute() {
  const { page = 0 } = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  return <HistoryPage page={page} onPageChange={(next) => navigate({ search: { page: next } })} />
}
