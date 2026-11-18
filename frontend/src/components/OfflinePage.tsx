import { WifiOff } from 'lucide-react'
import { useEffect } from 'react'
import { AuthCard } from '@/components/AuthCard'
import { Button } from '@/components/ui/button'
import { useOnline } from '@/lib/online'

/**
 * The offline fallback: the app shell comes from the service worker cache, but checking the
 * session needs the API. Back online, a full reload runs every guard again from a clean state
 * (it cannot loop: this page is only shown while the browser reports being offline).
 */
export function OfflinePage() {
  const online = useOnline()

  useEffect(() => {
    if (online) {
      window.location.reload()
    }
  }, [online])

  return (
    <AuthCard
      title="You are offline"
      description="Employee Portal needs a connection to load your account. It opens again by itself when you are back online."
    >
      <div className="grid justify-items-center gap-4">
        <WifiOff className="size-10 text-muted-foreground" aria-hidden />
        <Button className="w-full" onClick={() => window.location.reload()}>
          Try again
        </Button>
      </div>
    </AuthCard>
  )
}
