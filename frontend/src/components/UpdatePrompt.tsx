import { useEffect } from 'react'
import { toast } from 'sonner'
import { useRegisterSW } from 'virtual:pwa-register/react'

/**
 * Registers the service worker. When a new version is deployed, asks before reloading, so an
 * update never interrupts someone filling in a form.
 */
export function UpdatePrompt() {
  const {
    needRefresh: [needRefresh],
    updateServiceWorker,
  } = useRegisterSW()

  useEffect(() => {
    if (needRefresh) {
      toast('A new version is available', {
        id: 'app-update',
        duration: Infinity,
        action: { label: 'Reload', onClick: () => void updateServiceWorker(true) },
      })
    }
  }, [needRefresh, updateServiceWorker])

  return null
}
