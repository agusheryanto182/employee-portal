import { CircleCheck, CircleX, Monitor } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { describeFailure, describeUserAgent, formatDateTime } from '@/lib/format'
import type { LoginHistoryEntry } from '@/lib/types'

/** Login attempts as a list that works on a phone; failed attempts say why. */
export function LoginHistoryList({ entries }: { entries: LoginHistoryEntry[] }) {
  if (entries.length === 0) {
    return <p className="py-6 text-center text-sm text-muted-foreground">No sign-in attempts yet.</p>
  }
  return (
    <ul className="divide-y">
      {entries.map((entry) => (
        <li key={entry.id} className="flex items-start gap-3 py-3">
          {entry.success ? (
            <CircleCheck className="mt-0.5 size-5 shrink-0 text-emerald-600" aria-hidden />
          ) : (
            <CircleX className="mt-0.5 size-5 shrink-0 text-destructive" aria-hidden />
          )}
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
              <span className="font-medium">{entry.success ? 'Signed in' : describeFailure(entry.failureReason)}</span>
              <Badge variant={entry.success ? 'secondary' : 'destructive'}>{entry.success ? 'Success' : 'Failed'}</Badge>
            </div>
            <p className="mt-1 flex flex-wrap items-center gap-x-1.5 text-sm text-muted-foreground">
              <time dateTime={entry.createdAt}>{formatDateTime(entry.createdAt)}</time>
              <span aria-hidden>·</span>
              <span className="inline-flex items-center gap-1" title={entry.userAgent ?? undefined}>
                <Monitor className="size-3.5" aria-hidden />
                {describeUserAgent(entry.userAgent)}
              </span>
              {entry.ipAddress ? (
                <>
                  <span aria-hidden>·</span>
                  <span>IP {entry.ipAddress}</span>
                </>
              ) : null}
            </p>
          </div>
        </li>
      ))}
    </ul>
  )
}
