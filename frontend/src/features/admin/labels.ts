import type { Role, UserStatus } from '@/lib/types'

export const STATUS_LABELS: Record<UserStatus, string> = {
  ACTIVE: 'Active',
  PENDING: 'Invited',
  INACTIVE: 'Deactivated',
}

export const ROLE_LABELS: Record<Role, string> = { EMPLOYEE: 'Employee', ADMIN: 'Admin' }
