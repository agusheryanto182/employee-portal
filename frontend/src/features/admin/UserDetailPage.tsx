import { Link } from '@tanstack/react-router'
import { ArrowLeft, ArrowRight } from 'lucide-react'
import { toast } from 'sonner'
import { Pagination } from '@/components/Pagination'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { LoginHistoryList } from '@/features/history/LoginHistoryList'
import { toProblem } from '@/lib/api'
import { useSession } from '@/lib/auth'
import { formatDateTime } from '@/lib/format'
import type { User } from '@/lib/types'
import { useLoginHistory, useUpdateUser, useUser, type UserInput } from './queries'
import { UserActions } from './UserActions'
import { RoleBadge, StatusBadge } from './UserBadges'
import { UserForm } from './UserForm'

const HISTORY_SIZE = 10

interface UserDetailPageProps {
  userId: string
  historyPage: number
  onHistoryPageChange: (page: number) => void
}

export function UserDetailPage({ userId, historyPage, onHistoryPageChange }: UserDetailPageProps) {
  const user = useUser(userId)

  return (
    <div className="grid gap-6">
      <Button variant="ghost" size="sm" className="w-fit" asChild>
        <Link to="/admin/users">
          <ArrowLeft aria-hidden />
          All users
        </Link>
      </Button>

      {user.isPending ? (
        <Skeleton className="h-64 w-full" />
      ) : user.isError ? (
        <Alert variant="destructive">
          <AlertDescription>
            {toProblem(user.error).status === 404 ? 'This user does not exist.' : toProblem(user.error).detail}
          </AlertDescription>
        </Alert>
      ) : (
        <>
          <UserOverview user={user.data} />
          <UserHistory userId={userId} page={historyPage} onPageChange={onHistoryPageChange} />
        </>
      )}
    </div>
  )
}

function UserOverview({ user }: { user: User }) {
  const isSelf = useSession()?.user.id === user.id
  const update = useUpdateUser(user.id)

  /** Sends only what changed, so an untouched email does not trigger a new invite. */
  async function save(values: UserInput) {
    const changes: Partial<UserInput> = {}
    if (values.email !== user.email) changes.email = values.email
    if (values.fullName !== user.fullName) changes.fullName = values.fullName
    if (values.phone !== (user.phone ?? '')) changes.phone = values.phone
    if (!isSelf && values.role !== user.role) changes.role = values.role
    const updated = await update.mutateAsync(changes)
    toast.success(
      changes.email && updated.status === 'PENDING' ? `Saved. A new invite was sent to ${updated.email}` : 'Changes saved',
    )
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[3fr_2fr] lg:items-start">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl">{user.fullName}</CardTitle>
          <CardDescription>
            Added {formatDateTime(user.createdAt)} · Updated {formatDateTime(user.updatedAt)}
          </CardDescription>
          <CardAction className="flex gap-1.5">
            <RoleBadge role={user.role} />
            <StatusBadge status={user.status} />
          </CardAction>
        </CardHeader>
        <CardContent>
          <UserForm
            key={user.updatedAt} // start from the saved values after every change
            defaultValues={{ email: user.email, fullName: user.fullName, phone: user.phone ?? '', role: user.role }}
            submitLabel="Save changes"
            onSubmit={save}
            roleLocked={isSelf}
            emailHint={
              user.status === 'PENDING'
                ? 'Changing the email sends a new invite to the new address.'
                : 'Changing the email cancels any unused reset link.'
            }
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Account</CardTitle>
        </CardHeader>
        <CardContent>
          <UserActions user={user} isSelf={isSelf} />
        </CardContent>
      </Card>
    </div>
  )
}

function UserHistory({ userId, page, onPageChange }: { userId: string; page: number; onPageChange: (page: number) => void }) {
  const history = useLoginHistory({ userId, page }, HISTORY_SIZE)

  return (
    <Card>
      <CardHeader>
        <CardTitle>Login history</CardTitle>
        <CardDescription>Sign-in attempts on this account, newest first.</CardDescription>
        <CardAction>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/admin/login-history" search={{ userId }}>
              Filter
              <ArrowRight aria-hidden />
            </Link>
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent className="grid gap-4">
        {history.isPending ? (
          <Skeleton className="h-24 w-full" />
        ) : history.isError ? (
          <Alert variant="destructive">
            <AlertDescription>{toProblem(history.error).detail}</AlertDescription>
          </Alert>
        ) : (
          <>
            <LoginHistoryList entries={history.data.content} />
            <Pagination
              page={history.data.page}
              totalPages={history.data.totalPages}
              onPageChange={onPageChange}
              disabled={history.isFetching}
            />
          </>
        )}
      </CardContent>
    </Card>
  )
}
