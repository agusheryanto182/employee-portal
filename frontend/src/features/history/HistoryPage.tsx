import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Pagination } from '@/components/Pagination'
import { toProblem } from '@/lib/api'
import { LoginHistoryList } from './LoginHistoryList'
import { useMyLoginHistory } from './queries'

export const HISTORY_PAGE_SIZE = 20

interface HistoryPageProps {
  page: number
  onPageChange: (page: number) => void
}

export function HistoryPage({ page, onPageChange }: HistoryPageProps) {
  const history = useMyLoginHistory(page, HISTORY_PAGE_SIZE)

  return (
    <Card>
      <CardHeader>
        <CardTitle>Login history</CardTitle>
        <CardDescription>
          Every attempt to sign in to your account, including failed ones. If you see one you do not recognise, change
          your password and tell your administrator.
        </CardDescription>
      </CardHeader>
      <CardContent className="grid gap-4">
        {history.isPending ? (
          <div className="grid gap-3">
            {Array.from({ length: 5 }, (_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
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
