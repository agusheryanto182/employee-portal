import { WifiOff } from 'lucide-react'
import { useOnline } from '@/lib/online'

/** Shown on every screen while the device is offline; data reloads by itself when it is back. */
export function OfflineBanner() {
  const online = useOnline()
  if (online) {
    return null
  }
  return (
    <div
      role="status"
      className="flex items-center justify-center gap-2 bg-amber-100 px-4 py-2 text-sm text-amber-950 dark:bg-amber-950 dark:text-amber-100"
    >
      <WifiOff className="size-4 shrink-0" aria-hidden />
      You are offline. Changes need a connection; the page updates when you are back online.
    </div>
  )
}
