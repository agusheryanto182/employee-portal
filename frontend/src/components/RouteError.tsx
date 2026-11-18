import { useRouter, type ErrorComponentProps } from '@tanstack/react-router'
import { AuthCard } from '@/components/AuthCard'
import { OfflinePage } from '@/components/OfflinePage'
import { Button } from '@/components/ui/button'
import { OfflineError } from '@/lib/online'

/** Error screen for any route: the offline fallback, or a generic error with a retry. */
export function RouteError({ error }: ErrorComponentProps) {
  const router = useRouter()
  if (error instanceof OfflineError) {
    return <OfflinePage />
  }
  return (
    <AuthCard title="Something went wrong" description="The page could not be loaded. Please try again.">
      <Button className="w-full" onClick={() => void router.invalidate()}>
        Try again
      </Button>
    </AuthCard>
  )
}
