import { useSyncExternalStore } from 'react'

function subscribe(onChange: () => void) {
  window.addEventListener('online', onChange)
  window.addEventListener('offline', onChange)
  return () => {
    window.removeEventListener('online', onChange)
    window.removeEventListener('offline', onChange)
  }
}

/** `navigator.onLine` is only reliable when it says false, so it is used to explain failures, not to block requests. */
export function isOffline(): boolean {
  return typeof navigator !== 'undefined' && !navigator.onLine
}

export function useOnline(): boolean {
  return useSyncExternalStore(subscribe, () => navigator.onLine, () => true)
}

/**
 * Thrown by a route guard when the session cannot be checked because the device is offline. The
 * root route shows the offline screen instead of sending a signed-in user to the sign-in page.
 */
export class OfflineError extends Error {
  constructor() {
    super('You are offline')
    this.name = 'OfflineError'
  }
}
