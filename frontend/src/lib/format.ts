import type { LoginFailureReason } from './types'

const dateTime = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso))
}

const FAILURE_REASONS: Record<LoginFailureReason, string> = {
  INVALID_CREDENTIALS: 'Wrong email or password',
  ACCOUNT_PENDING: 'Account not activated yet',
  ACCOUNT_INACTIVE: 'Account deactivated',
  WRONG_PORTAL: 'Used the other sign-in page',
  RATE_LIMITED: 'Blocked after too many attempts',
}

export function describeFailure(reason: LoginFailureReason | null): string {
  return reason ? FAILURE_REASONS[reason] : 'Failed'
}

/** "Chrome on Windows" from a user agent string; good enough for a login history row. */
export function describeUserAgent(userAgent: string | null): string {
  if (!userAgent) {
    return 'Unknown device'
  }
  const browser =
    /Edg\//.test(userAgent) ? 'Edge'
    : /OPR\//.test(userAgent) ? 'Opera'
    : /Firefox\//.test(userAgent) ? 'Firefox'
    : /Chrome\//.test(userAgent) ? 'Chrome'
    : /Safari\//.test(userAgent) ? 'Safari'
    : /curl\//.test(userAgent) ? 'curl'
    : null
  const os =
    /Android/.test(userAgent) ? 'Android'
    : /iPhone|iPad|iPod/.test(userAgent) ? 'iOS'
    : /Windows/.test(userAgent) ? 'Windows'
    : /Mac OS X|Macintosh/.test(userAgent) ? 'macOS'
    : /Linux/.test(userAgent) ? 'Linux'
    : null
  if (browser && os) {
    return `${browser} on ${os}`
  }
  return browser ?? os ?? 'Unknown device'
}

export function initials(fullName: string): string {
  return fullName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]!.toUpperCase())
    .join('')
}
