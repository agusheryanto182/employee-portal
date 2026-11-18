import { Link } from '@tanstack/react-router'
import { X } from 'lucide-react'
import { FormField } from '@/components/FormField'
import { NativeSelect } from '@/components/NativeSelect'
import { Pagination } from '@/components/Pagination'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { LoginHistoryList } from '@/features/history/LoginHistoryList'
import { toProblem } from '@/lib/api'
import type { LoginHistoryEntry } from '@/lib/types'
import { useLoginHistory, useUser, type HistoryFilters } from './queries'

interface LoginHistoryPageProps {
  filters: HistoryFilters
  onFiltersChange: (filters: HistoryFilters) => void
}

/** Every sign-in attempt on any account, including unknown emails (ADR 0005). */
export function LoginHistoryPage({ filters, onFiltersChange }: LoginHistoryPageProps) {
  const history = useLoginHistory(filters)
  const change = (next: Partial<HistoryFilters>) => onFiltersChange({ ...filters, ...next, page: undefined })
  const hasFilters = Boolean(filters.userId || filters.outcome || filters.from || filters.to)

  return (
    <Card>
      <CardHeader>
        <CardTitle>Login history</CardTitle>
        <CardDescription>
          Every attempt to sign in to the dashboard or the backoffice, including emails that are not registered.
        </CardDescription>
      </CardHeader>
      <CardContent className="grid gap-4">
        {filters.userId ? <UserFilterChip userId={filters.userId} onClear={() => change({ userId: undefined })} /> : null}

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-[1fr_1fr_1fr_auto] sm:items-end">
          <div className="col-span-2 sm:col-span-1">
            <FormField id="outcome" label="Result">
              <NativeSelect
                id="outcome"
                value={filters.outcome ?? ''}
                onChange={(event) =>
                  change({ outcome: (event.target.value || undefined) as HistoryFilters['outcome'] })
                }
              >
                <option value="">All attempts</option>
                <option value="success">Successful</option>
                <option value="failed">Failed</option>
              </NativeSelect>
            </FormField>
          </div>
          <FormField id="from" label="From">
            <Input
              id="from"
              type="date"
              value={filters.from ?? ''}
              max={filters.to}
              onChange={(event) => change({ from: event.target.value || undefined })}
            />
          </FormField>
          <FormField id="to" label="To">
            <Input
              id="to"
              type="date"
              value={filters.to ?? ''}
              min={filters.from}
              onChange={(event) => change({ to: event.target.value || undefined })}
            />
          </FormField>
          {hasFilters ? (
            <Button variant="ghost" className="col-span-2 sm:col-span-1" onClick={() => onFiltersChange({})}>
              Clear filters
            </Button>
          ) : null}
        </div>

        {history.isPending ? (
          <div className="grid gap-3">
            {Array.from({ length: 5 }, (_, i) => (
              <Skeleton key={i} className="h-14 w-full" />
            ))}
          </div>
        ) : history.isError ? (
          <Alert variant="destructive">
            <AlertDescription>{toProblem(history.error).detail}</AlertDescription>
          </Alert>
        ) : (
          <>
            <LoginHistoryList
              entries={history.data.content}
              renderAccount={(entry) => <AccountLink entry={entry} />}
              emptyMessage={hasFilters ? 'No attempts match these filters.' : 'No sign-in attempts yet.'}
            />
            <Pagination
              page={history.data.page}
              totalPages={history.data.totalPages}
              onPageChange={(page) => onFiltersChange({ ...filters, page })}
              disabled={history.isFetching}
            />
          </>
        )}
      </CardContent>
    </Card>
  )
}

/** Attempts for a registered account link to it; unknown emails are shown as typed. */
function AccountLink({ entry }: { entry: LoginHistoryEntry }) {
  if (!entry.userId) {
    return (
      <span className="text-muted-foreground">
        {entry.email} <span className="text-xs">(not registered)</span>
      </span>
    )
  }
  return (
    <Link to="/admin/users/$userId" params={{ userId: entry.userId }} className="hover:underline">
      {entry.email}
    </Link>
  )
}

function UserFilterChip({ userId, onClear }: { userId: string; onClear: () => void }) {
  const user = useUser(userId)
  const name = user.data ? `${user.data.fullName} (${user.data.email})` : 'one user'
  return (
    <div className="flex items-center gap-2 rounded-lg border bg-muted/50 px-3 py-2 text-sm">
      <span className="min-w-0 flex-1 truncate">Showing attempts for {name}</span>
      <Button variant="ghost" size="icon-sm" aria-label="Show all users" onClick={onClear}>
        <X aria-hidden />
      </Button>
    </div>
  )
}
