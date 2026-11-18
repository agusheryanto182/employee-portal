import { Badge } from '@/components/ui/badge'
import type { Role, UserStatus } from '@/lib/types'
import { ROLE_LABELS, STATUS_LABELS } from './labels'

const STATUS_VARIANT: Record<UserStatus, 'secondary' | 'outline' | 'destructive'> = {
  ACTIVE: 'secondary',
  PENDING: 'outline',
  INACTIVE: 'destructive',
}

export function StatusBadge({ status }: { status: UserStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{STATUS_LABELS[status]}</Badge>
}

export function RoleBadge({ role }: { role: Role }) {
  return <Badge variant={role === 'ADMIN' ? 'default' : 'ghost'}>{ROLE_LABELS[role]}</Badge>
}
