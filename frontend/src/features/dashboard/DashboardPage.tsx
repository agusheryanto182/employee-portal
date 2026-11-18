import { Link } from '@tanstack/react-router'
import { ArrowRight, Mail, Phone } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { toProblem } from '@/lib/api'
import type { User } from '@/lib/types'
import { LoginHistoryList } from '@/features/history/LoginHistoryList'
import { useMyLoginHistory } from '@/features/history/queries'

const RECENT = 5

export function DashboardPage({ user }: { user: User }) {
  const recent = useMyLoginHistory(0, RECENT)
  const firstName = user.fullName.split(/\s+/)[0]

  return (
    <div className="grid gap-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl">Welcome back, {firstName}</CardTitle>
          <CardDescription>Here is a summary of your account.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-2 text-sm">
          <p className="flex items-center gap-2">
            <Mail className="size-4 text-muted-foreground" aria-hidden />
            {user.email}
          </p>
          <p className="flex items-center gap-2">
            <Phone className="size-4 text-muted-foreground" aria-hidden />
            {user.phone ?? <span className="text-muted-foreground">No phone number</span>}
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Recent sign-ins</CardTitle>
          <CardDescription>The last {RECENT} attempts on your account.</CardDescription>
          <CardAction>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/history">
                View all
                <ArrowRight aria-hidden />
              </Link>
            </Button>
          </CardAction>
        </CardHeader>
        <CardContent>
          {recent.isPending ? (
            <div className="grid gap-3">
              {Array.from({ length: 3 }, (_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : recent.isError ? (
            <Alert variant="destructive">
              <AlertDescription>{toProblem(recent.error).detail}</AlertDescription>
            </Alert>
          ) : (
            <LoginHistoryList entries={recent.data.content} />
          )}
        </CardContent>
      </Card>
    </div>
  )
}
