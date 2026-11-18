import { Link } from '@tanstack/react-router'
import { Search, UserPlus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { NativeSelect } from '@/components/NativeSelect'
import { Pagination } from '@/components/Pagination'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { toProblem } from '@/lib/api'
import { formatDateTime } from '@/lib/format'
import type { Role, User, UserStatus } from '@/lib/types'
import { useDebouncedValue } from '@/lib/useDebouncedValue'
import { useUsers, type UserFilters } from './queries'
import { ROLE_LABELS, STATUS_LABELS } from './labels'
import { RoleBadge, StatusBadge } from './UserBadges'

interface UsersPageProps {
  filters: UserFilters
  /** Filters live in the URL, so a filtered list can be shared and survives a reload. */
  onFiltersChange: (filters: UserFilters) => void
}

export function UsersPage({ filters, onFiltersChange }: UsersPageProps) {
  const users = useUsers(filters)
  const [search, setSearch] = useState(filters.q ?? '')
  const debouncedSearch = useDebouncedValue(search.trim())

  useEffect(() => {
    if (debouncedSearch !== (filters.q ?? '')) {
      onFiltersChange({ ...filters, q: debouncedSearch || undefined, page: undefined })
    }
  }, [debouncedSearch]) // eslint-disable-line react-hooks/exhaustive-deps -- only react to typing

  const hasFilters = Boolean(filters.q || filters.status || filters.role)

  return (
    <Card>
      <CardHeader>
        <CardTitle>Users</CardTitle>
        <CardDescription>Everyone who can sign in to the dashboard or the backoffice.</CardDescription>
        <CardAction>
          <Button asChild>
            <Link to="/admin/users/new">
              <UserPlus aria-hidden />
              Add user
            </Link>
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent className="grid gap-4">
        <div className="grid gap-2 sm:grid-cols-[1fr_auto_auto]">
          <div className="relative">
            <Search
              className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              type="search"
              aria-label="Search by name or email"
              placeholder="Search by name or email"
              className="pl-8"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <NativeSelect
            aria-label="Status"
            value={filters.status ?? ''}
            onChange={(event) =>
              onFiltersChange({ ...filters, status: (event.target.value || undefined) as UserStatus, page: undefined })
            }
          >
            <option value="">All statuses</option>
            {(Object.keys(STATUS_LABELS) as UserStatus[]).map((status) => (
              <option key={status} value={status}>
                {STATUS_LABELS[status]}
              </option>
            ))}
          </NativeSelect>
          <NativeSelect
            aria-label="Role"
            value={filters.role ?? ''}
            onChange={(event) =>
              onFiltersChange({ ...filters, role: (event.target.value || undefined) as Role, page: undefined })
            }
          >
            <option value="">All roles</option>
            {(Object.keys(ROLE_LABELS) as Role[]).map((role) => (
              <option key={role} value={role}>
                {ROLE_LABELS[role]}
              </option>
            ))}
          </NativeSelect>
        </div>

        {users.isPending ? (
          <div className="grid gap-3">
            {Array.from({ length: 5 }, (_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : users.isError ? (
          <Alert variant="destructive">
            <AlertDescription>{toProblem(users.error).detail}</AlertDescription>
          </Alert>
        ) : users.data.content.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            {hasFilters ? 'No users match these filters.' : 'No users yet.'}
          </p>
        ) : (
          <>
            <UsersTable users={users.data.content} />
            <p className="text-sm text-muted-foreground">
              {users.data.totalElements} {users.data.totalElements === 1 ? 'user' : 'users'}
            </p>
            <Pagination
              page={users.data.page}
              totalPages={users.data.totalPages}
              onPageChange={(page) => onFiltersChange({ ...filters, page })}
              disabled={users.isFetching}
            />
          </>
        )}
      </CardContent>
    </Card>
  )
}

/** One table for every screen size: on phones the email moves under the name and secondary columns hide. */
function UsersTable({ users }: { users: User[] }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Name</TableHead>
          <TableHead className="hidden md:table-cell">Email</TableHead>
          <TableHead className="hidden sm:table-cell">Role</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="hidden lg:table-cell">Added</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {users.map((user) => (
          <TableRow key={user.id} className="relative">
            <TableCell className="max-w-0 w-full md:w-auto md:max-w-none">
              {/* The link covers the whole row, so the row is one big tap target. */}
              <Link
                to="/admin/users/$userId"
                params={{ userId: user.id }}
                className="block truncate font-medium after:absolute after:inset-0 hover:underline"
              >
                {user.fullName}
              </Link>
              <span className="block truncate text-xs text-muted-foreground md:hidden">{user.email}</span>
            </TableCell>
            <TableCell className="hidden md:table-cell">{user.email}</TableCell>
            <TableCell className="hidden sm:table-cell">
              <RoleBadge role={user.role} />
            </TableCell>
            <TableCell>
              <StatusBadge status={user.status} />
            </TableCell>
            <TableCell className="hidden text-muted-foreground lg:table-cell">
              <time dateTime={user.createdAt}>{formatDateTime(user.createdAt)}</time>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
